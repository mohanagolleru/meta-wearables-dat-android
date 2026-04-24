/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

// StreamViewModel - MedSpect Camera + Audio Streaming
//
// Streams camera frames AND glasses mic audio to the Mac over WebSocket,
// receives Gemini response audio back, and plays it through the glasses speakers.
//
// Video: WDAT camera → I420 → NV21 → JPEG → WS (b'V' prefix) → Mac → Gemini
// Audio: Glasses mic → BT SCO → AudioRecord → WS (b'A' prefix) → Mac → Gemini
//        Gemini → Mac → WS (b'A' prefix) → AudioTrack → BT SCO → Glasses speakers

package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.meta.wearable.dat.camera.StreamSession
import com.meta.wearable.dat.camera.startStreamSession
import com.meta.wearable.dat.camera.types.PhotoData
import com.meta.wearable.dat.camera.types.StreamConfiguration
import com.meta.wearable.dat.camera.types.StreamSessionState
import com.meta.wearable.dat.camera.types.VideoFrame
import com.meta.wearable.dat.camera.types.VideoQuality
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.selectors.DeviceSelector
import com.meta.wearable.dat.externalsampleapps.cameraaccess.wearables.WearablesViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StreamViewModel(
    application: Application,
    private val wearablesViewModel: WearablesViewModel,
) : AndroidViewModel(application) {

  companion object {
    private const val TAG = "StreamViewModel"
    private const val WS_URL = "ws://127.0.0.1:8765"
    private val INITIAL_STATE = StreamUiState()
  }

  private val deviceSelector: DeviceSelector = wearablesViewModel.deviceSelector
  private var streamSession: StreamSession? = null

  // Bidirectional relay: video + audio to Mac, response audio from Mac
  private val wsSender = WsBinarySender(WS_URL)

  // Bluetooth SCO audio bridge for glasses mic/speaker
  private val audioBridge = BluetoothAudioBridge(application)

  private var lastWsSentMs: Long = 0L
  // Reusable ByteArrayOutputStream — avoids allocating a new one per frame
  private val jpegBuffer = ByteArrayOutputStream(32_768)
  private val _uiState = MutableStateFlow(INITIAL_STATE)
  val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

  // Audio level for voice orb visualization (0.0–1.0)
  private val _audioLevel = MutableStateFlow(0f)
  val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

  private var audioLevelJob: Job? = null

  private var videoJob: Job? = null
  private var stateJob: Job? = null

  private var frameCounter: Long = 0
  private var wsSendFailCount: Int = 0
  private var audioSendFailCount: Int = 0

  fun startStream() {
    // Re-entry guard: if a session is already alive, a duplicate startStream() call
    // (e.g. LaunchedEffect firing twice on recomposition) would race with async SDK cleanup
    // and trigger error code 1300 ("cannot process request from the current state, STOPPED").
    // The live session will either stream successfully, or its stateJob will call stopStream()
    // → navigateToDeviceSelection() when it finishes — no need to restart it here.
    if (streamSession != null) {
      Log.d(TAG, "startStream: session already active, ignoring duplicate call")
      return
    }

    videoJob?.cancel()
    stateJob?.cancel()
    streamSession = null

    _uiState.update { it.copy(streamError = null) }

    // Connect WS first so we are ready when frames arrive
    wsSender.connect()

    // Wire audio: glasses mic → WS → Mac, and Mac → WS → glasses speakers
    audioSendFailCount = 0
    audioBridge.onAudioCaptured = { pcmData ->
      if (!wsSender.sendAudio(pcmData)) {
        audioSendFailCount++
        // Throttle: log first failure, then every 100th to avoid spam
        if (audioSendFailCount == 1 || audioSendFailCount % 100 == 0) {
          Log.w(TAG, "audio send failed x$audioSendFailCount — WS may be disconnected")
        }
      } else if (audioSendFailCount > 0) {
        Log.i(TAG, "audio send recovered after $audioSendFailCount failures")
        audioSendFailCount = 0
      }
    }
    wsSender.onAudioReceived = { pcmData -> audioBridge.playAudio(pcmData) }

    // Fix 8: startCapture() returns false on failure (permissions, BT unavailable, etc.)
    if (!audioBridge.startCapture()) {
      Log.e(TAG, "Audio capture failed to start — continuing with video only")
    } else {
      // Poll audio level for voice orb at ~15 FPS
      audioLevelJob = viewModelScope.launch {
        while (isActive) {
          _audioLevel.value = audioBridge.currentRmsLevel
          delay(66) // ~15 FPS
        }
      }
    }

    val session = try {
      Wearables.startStreamSession(
              getApplication(),
              deviceSelector,
              StreamConfiguration(videoQuality = VideoQuality.LOW, 10),
          )
          .also { streamSession = it }
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to start stream session", t)
      // Null callbacks before stopping — prevents late events from hitting released resources
      wsSender.onAudioReceived = null
      audioBridge.onAudioCaptured = null
      audioBridge.stopCapture()
      wsSender.close()   // don't leave WS open when session creation failed
      _uiState.update { it.copy(streamError = "Stream failed to start: ${t.message}") }
      wearablesViewModel.navigateToDeviceSelection()   // flip isStreaming→false so UI reflects reality
      return
    }

    // Heavy work (YUV conversion, JPEG encode, bitmap decode) runs on IO dispatcher
    // to avoid stalling the main thread / Compose recomposition.
    videoJob =
        viewModelScope.launch(Dispatchers.IO) {
          try {
            session.videoStream.collect { handleVideoFrame(it) }
          } catch (t: Throwable) {
            Log.e(TAG, "videoStream collect error", t)
            _uiState.update { it.copy(streamError = "Frame stream error: ${t.message}") }
          }
        }

    stateJob =
        viewModelScope.launch {
          session.state.collect { currentState ->
            val prevState = _uiState.value.streamSessionState
            _uiState.update { it.copy(streamSessionState = currentState) }

            // navigate back when state transitioned to STOPPED
            if (currentState != prevState && currentState == StreamSessionState.STOPPED) {
              stopStream()
              wearablesViewModel.navigateToDeviceSelection()
            }
          }
        }
  }

  fun stopStream() {
    // Null callbacks FIRST — before anything that could trigger late events.
    // streamSession?.close() and coroutine cancellation can still fire callbacks
    // on the IO/OkHttp threads, so sever the wiring up front.
    wsSender.onAudioReceived = null
    audioBridge.onAudioCaptured = null

    audioLevelJob?.cancel()
    audioLevelJob = null
    _audioLevel.value = 0f

    videoJob?.cancel()
    videoJob = null
    stateJob?.cancel()
    stateJob = null

    try {
      streamSession?.close()
    } catch (t: Throwable) {
      Log.w(TAG, "streamSession close failed", t)
    }
    streamSession = null

    audioBridge.stopCapture()
    wsSender.close()

    _uiState.update { INITIAL_STATE }
  }

  fun capturePhoto() {
    if (uiState.value.isCapturing) {
      Log.d(TAG, "Photo capture already in progress, ignoring request")
      return
    }

    if (uiState.value.streamSessionState == StreamSessionState.STREAMING) {
      Log.d(TAG, "Starting photo capture")
      _uiState.update { it.copy(isCapturing = true) }

      viewModelScope.launch {
        streamSession
            ?.capturePhoto()
            ?.onSuccess { photoData ->
              Log.d(TAG, "Photo capture successful")
              handlePhotoData(photoData)
              _uiState.update { it.copy(isCapturing = false) }
            }
            ?.onFailure { err ->
              Log.e(TAG, "Photo capture failed", err)
              _uiState.update { it.copy(isCapturing = false) }
            }
      }
    } else {
      Log.w(
          TAG,
          "Cannot capture photo: stream not active (state=${uiState.value.streamSessionState})",
      )
    }
  }

  fun showShareDialog() {
    _uiState.update { it.copy(isShareDialogVisible = true) }
  }

  fun hideShareDialog() {
    _uiState.update { it.copy(isShareDialogVisible = false) }
  }

  fun sharePhoto(bitmap: Bitmap) {
    val context = getApplication<Application>()
    val imagesFolder = File(context.cacheDir, "images")
    try {
      imagesFolder.mkdirs()
      val file = File(imagesFolder, "shared_image.png")
      FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
      }

      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val intent = Intent(Intent.ACTION_SEND)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      intent.putExtra(Intent.EXTRA_STREAM, uri)
      intent.type = "image/png"
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

      val chooser = Intent.createChooser(intent, "Share Image")
      chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(chooser)
    } catch (e: IOException) {
      Log.e(TAG, "Failed to share photo", e)
    }
  }

  private fun handleVideoFrame(videoFrame: VideoFrame) {
    // VideoFrame contains raw I420 video data in a ByteBuffer
    val buffer = videoFrame.buffer
    val w = videoFrame.width
    val h = videoFrame.height
    val dataSize = buffer.remaining()

    // MEDIUM FIX: validate I420 frame size (Y + U + V = w*h * 3/2)
    val expectedSize = w * h * 3 / 2
    if (dataSize < expectedSize) {
      Log.w(TAG, "I420 frame too small: got $dataSize, expected $expectedSize for ${w}x${h}")
      return
    }

    val byteArray = ByteArray(dataSize)
    val originalPosition = buffer.position()
    buffer.get(byteArray)
    buffer.position(originalPosition)

    frameCounter++

    // Throttle: only JPEG-encode + send at ~1 fps to avoid wasting CPU
    val now = System.currentTimeMillis()
    val shouldSend = now - lastWsSentMs >= 1000

    // Convert I420 → NV21 (supported by YuvImage)
    val nv21 = convertI420toNV21(byteArray, w, h)
    val image = YuvImage(nv21, ImageFormat.NV21, w, h, null)

    // MEDIUM FIX: reuse ByteArrayOutputStream instead of allocating per frame
    jpegBuffer.reset()
    val compressed = image.compressToJpeg(Rect(0, 0, w, h), 50, jpegBuffer)
    if (!compressed) {
      Log.w(TAG, "compressToJpeg failed for frame $frameCounter — dropping frame")
      return
    }
    val jpegBytes = jpegBuffer.toByteArray()

    // Send to Mac over WS at ~1 fps
    if (shouldSend) {
      lastWsSentMs = now
      val sent = wsSender.sendVideoJpeg(jpegBytes)
      if (!sent) {
        wsSendFailCount++
        if (wsSendFailCount == 1 || wsSendFailCount % 5 == 0) {
          Log.w(TAG, "WS send failing (attempt $wsSendFailCount, frame $frameCounter, ${jpegBytes.size}B) — check adb reverse + proxy")
        }
      } else if (wsSendFailCount > 0) {
        Log.i(TAG, "WS send recovered after $wsSendFailCount consecutive failures")
        wsSendFailCount = 0
      }
    }

    // Update UI preview (BitmapFactory on IO thread is fine; StateFlow update is thread-safe)
    val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    if (bitmap != null) {
      // Do NOT call old.recycle() — Compose may still be rendering the previous frame
      // on the UI thread when this IO-thread code runs. At 1 fps and small (640px) frames,
      // GC pressure from letting the old bitmap be collected naturally is negligible.
      _uiState.update { it.copy(videoFrame = bitmap) }
    }
  }

  // Convert I420 (YYYYYYYY:UUVV) to NV21 (YYYYYYYY:VUVU)
  private fun convertI420toNV21(input: ByteArray, width: Int, height: Int): ByteArray {
    val output = ByteArray(input.size)
    val size = width * height
    val quarter = size / 4

    input.copyInto(output, 0, 0, size) // Y is the same

    for (n in 0 until quarter) {
      output[size + n * 2] = input[size + quarter + n] // V first
      output[size + n * 2 + 1] = input[size + n] // U second
    }
    return output
  }

  private fun handlePhotoData(photo: PhotoData) {
    val capturedPhoto =
        when (photo) {
          is PhotoData.Bitmap -> photo.bitmap
          is PhotoData.HEIC -> {
            val byteArray = ByteArray(photo.data.remaining())
            photo.data.get(byteArray)

            // Extract EXIF transformation matrix and apply to bitmap
            val exifInfo = getExifInfo(byteArray)
            val transform = getTransform(exifInfo)
            decodeHeic(byteArray, transform)
          }
        }
    _uiState.update { it.copy(capturedPhoto = capturedPhoto, isShareDialogVisible = true) }
  }

  // HEIC Decoding with EXIF transformation
  private fun decodeHeic(heicBytes: ByteArray, transform: Matrix): Bitmap {
    val bitmap = BitmapFactory.decodeByteArray(heicBytes, 0, heicBytes.size)
    return applyTransform(bitmap, transform)
  }

  private fun getExifInfo(heicBytes: ByteArray): ExifInterface? {
    return try {
      ByteArrayInputStream(heicBytes).use { inputStream -> ExifInterface(inputStream) }
    } catch (e: IOException) {
      Log.w(TAG, "Failed to read EXIF from HEIC", e)
      null
    }
  }

  private fun getTransform(exifInfo: ExifInterface?): Matrix {
    val matrix = Matrix()

    if (exifInfo == null) {
      return matrix
    }

    when (
        exifInfo.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    ) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.postRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.postRotate(270f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_NORMAL,
      ExifInterface.ORIENTATION_UNDEFINED -> {
        // no-op
      }
    }

    return matrix
  }

  private fun applyTransform(bitmap: Bitmap, matrix: Matrix): Bitmap {
    if (matrix.isIdentity) return bitmap

    return try {
      val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      if (transformed != bitmap) bitmap.recycle()
      transformed
    } catch (e: OutOfMemoryError) {
      Log.e(TAG, "Failed to apply transformation due to memory", e)
      bitmap
    }
  }

  override fun onCleared() {
    super.onCleared()
    stopStream()
    // Permanently shut down OkHttpClient threads — only safe here because the
    // ViewModel (and its WsBinarySender) are being destroyed.
    wsSender.destroy()
  }

  class Factory(
      private val application: Application,
      private val wearablesViewModel: WearablesViewModel,
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(StreamViewModel::class.java)) {
        @Suppress("UNCHECKED_CAST", "KotlinGenericsCast")
        return StreamViewModel(
            application = application,
            wearablesViewModel = wearablesViewModel,
        ) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
