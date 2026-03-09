package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Bidirectional WebSocket relay between the phone and the Mac.
 *
 * Send protocol (phone → Mac):
 *   b'V' + jpeg_bytes   — video frame from glasses camera
 *   b'A' + pcm16_bytes  — mic audio from glasses (16 kHz mono PCM16)
 *
 * Receive protocol (Mac → phone):
 *   b'A' + pcm16_bytes  — Gemini response audio (24 kHz mono PCM16)
 */
class WsBinarySender(private val wsUrl: String) {
  private val client = OkHttpClient()
  private var ws: WebSocket? = null
  private val isOpen = AtomicBoolean(false)
  private val isConnecting = AtomicBoolean(false)

  /** Called when response audio arrives from the Mac (Gemini playback). */
  var onAudioReceived: ((ByteArray) -> Unit)? = null

  fun connect() {
    // Skip if already open, or if a connection attempt is already in-flight
    if (isOpen.get() || !isConnecting.compareAndSet(false, true)) return
    val req = Request.Builder().url(wsUrl).build()
    ws = client.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        isOpen.set(true)
        isConnecting.set(false)
        Log.i(TAG, "WS open url=$wsUrl")
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val data = bytes.toByteArray()
        if (data.isNotEmpty() && data[0] == 'A'.code.toByte()) {
          // Audio response from Mac — strip prefix, deliver to bridge
          val audio = data.copyOfRange(1, data.size)
          onAudioReceived?.invoke(audio)
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        isOpen.set(false)
        isConnecting.set(false)
        Log.w(TAG, "WS closed code=$code reason=$reason")
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        isOpen.set(false)
        isConnecting.set(false)
        Log.e(TAG, "WS failure: ${t.message}", t)
      }
    })
  }

  fun sendVideoJpeg(jpeg: ByteArray): Boolean {
    if (!isOpen.get()) {
      // lazy connect, do not block the frame thread
      connect()
      return false
    }
    val msg = ByteArray(1 + jpeg.size)
    msg[0] = 'V'.code.toByte()
    System.arraycopy(jpeg, 0, msg, 1, jpeg.size)
    val sent = ws?.send(ByteString.of(*msg)) ?: false
    if (!sent) {
      isOpen.set(false)
    }
    return sent
  }

  /** Send mic audio captured from the glasses to the Mac. */
  fun sendAudio(pcmData: ByteArray): Boolean {
    if (!isOpen.get()) return false
    val msg = ByteArray(1 + pcmData.size)
    msg[0] = 'A'.code.toByte()
    System.arraycopy(pcmData, 0, msg, 1, pcmData.size)
    return ws?.send(ByteString.of(*msg)) ?: false
  }

  fun close() {
    onAudioReceived = null   // Fix 6: prevent late callbacks to released AudioTrack
    try { ws?.close(1000, "bye") } catch (_: Exception) {}
    isOpen.set(false)
    isConnecting.set(false)
  }

  companion object {
    private const val TAG = "WsBinarySender"
  }
}
