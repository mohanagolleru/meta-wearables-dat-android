/*
 * BluetoothAudioBridge — Routes audio through Ray-Ban Meta glasses via Bluetooth SCO/HFP.
 *
 * When the glasses are paired via HFP, this bridge:
 *   - Captures mic audio from the glasses (AudioRecord over SCO)
 *   - Plays response audio through the glasses speakers (AudioTrack over SCO)
 *
 * SCO connection is started asynchronously — audio begins with the phone mic/speaker
 * and automatically switches to the glasses when Bluetooth SCO connects.
 *
 * Audio formats (matching Gemini Live API expectations):
 *   Input:  16 kHz, mono, PCM 16-bit
 *   Output: 24 kHz, mono, PCM 16-bit
 */

package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class BluetoothAudioBridge(private val context: Context) {

    companion object {
        private const val TAG = "BtAudioBridge"
        private const val INPUT_SAMPLE_RATE = 16000
        private const val OUTPUT_SAMPLE_RATE = 24000
        // Accumulate at least 100ms of audio before sending (16kHz * 1ch * 2 bytes * 0.1s)
        private const val MIN_SEND_BYTES = 3200
    }

    /** Called with accumulated PCM chunks captured from the glasses mic. */
    @Volatile var onAudioCaptured: ((ByteArray) -> Unit)? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var captureThread: Thread? = null
    @Volatile private var isCapturing = false

    private val accumulatedData = ByteArrayOutputStream()
    private val accumulateLock = Any()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var scoReceiver: BroadcastReceiver? = null
    @Volatile private var scoConnected = false

    // ── Public API ──────────────────────────────────────────────────

    /**
     * Start capturing audio from the glasses mic and prepare playback.
     *
     * SCO connection is started asynchronously — audio begins with the phone mic/speaker
     * and automatically switches to the glasses when Bluetooth SCO connects.
     * This avoids blocking the main thread (which would deadlock the BroadcastReceiver).
     *
     * Returns true if capture started successfully, false on failure
     * (missing permissions, AudioRecord init failure, etc.)
     */
    @SuppressLint("MissingPermission")
    fun startCapture(): Boolean {
        if (isCapturing) return true

        // Fix 5: Runtime permission check
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing RECORD_AUDIO permission — cannot start capture")
            return false
        }

        // Start SCO asynchronously — audio starts with phone mic fallback and switches
        // to glasses when SCO connects. We do NOT block here because startCapture() is
        // called from the main thread and BroadcastReceiver.onReceive() also dispatches
        // on the main looper — blocking would deadlock.
        startSco()
        Log.d(TAG, "SCO requested (async) — will use phone mic until glasses connect")

        // Fix 2: Validate AudioRecord initialization
        val bufferSize = AudioRecord.getMinBufferSize(
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) {
            Log.e(TAG, "getMinBufferSize failed: $bufferSize")
            stopSco()
            return false
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            INPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize (state=${record.state})")
            record.release()
            stopSco()
            return false
        }
        audioRecord = record

        // Fix 2: Validate AudioTrack initialization
        val trackMinBuffer = AudioTrack.getMinBufferSize(
            OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (trackMinBuffer <= 0) {
            Log.e(TAG, "AudioTrack getMinBufferSize failed: $trackMinBuffer")
            record.release()
            audioRecord = null
            stopSco()
            return false
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(OUTPUT_SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(trackMinBuffer * 2)
            .build()

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack failed to initialize (state=${track.state})")
            track.release()
            record.release()
            audioRecord = null
            stopSco()
            return false
        }
        audioTrack = track

        record.startRecording()
        track.play()
        isCapturing = true

        synchronized(accumulateLock) {
            accumulatedData.reset()
        }

        captureThread = Thread({
            val buffer = ByteArray(bufferSize)
            while (isCapturing) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read < 0) {
                    Log.e(TAG, "AudioRecord.read error: $read")
                    break
                }
                if (read > 0) {
                    // Fix 3: Snapshot chunk inside lock, invoke callback OUTSIDE lock
                    val chunk: ByteArray?
                    synchronized(accumulateLock) {
                        accumulatedData.write(buffer, 0, read)
                        chunk = if (accumulatedData.size() >= MIN_SEND_BYTES) {
                            accumulatedData.toByteArray().also { accumulatedData.reset() }
                        } else {
                            null
                        }
                    }
                    chunk?.let { onAudioCaptured?.invoke(it) }
                }
            }
        }, "bt-audio-capture").also { it.start() }

        Log.d(TAG, "Audio capture started (16kHz mono PCM16, SCO=${scoConnected})")
        return true
    }

    /** Play Gemini response audio through the glasses speakers. */
    fun playAudio(data: ByteArray) {
        val track = audioTrack ?: return
        if (!isCapturing || data.isEmpty()) return
        // MEDIUM FIX: check write return value for errors
        val written = track.write(data, 0, data.size)
        if (written < 0) {
            Log.w(TAG, "AudioTrack.write error: $written")
        }
    }

    /** Flush playback buffer (e.g. on interruption). */
    fun flushPlayback() {
        val track = audioTrack ?: return
        try {
            track.pause()
            track.flush()
            track.play()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "flushPlayback failed: ${e.message}")
        }
    }

    fun stopCapture() {
        if (!isCapturing) return
        isCapturing = false

        // Stop AudioRecord first so the capture thread's read() unblocks
        audioRecord?.stop()

        captureThread?.join(2000)
        captureThread = null

        // Flush remaining accumulated audio if callback is still wired.
        // (StreamViewModel nulls onAudioCaptured before calling stopCapture,
        //  so this is a safety net for direct stopCapture() calls.)
        val flushedChunk: ByteArray?
        synchronized(accumulateLock) {
            flushedChunk = if (accumulatedData.size() > 0) {
                accumulatedData.toByteArray().also { accumulatedData.reset() }
            } else {
                null
            }
        }
        flushedChunk?.let { onAudioCaptured?.invoke(it) }

        audioRecord?.release()
        audioRecord = null

        // MEDIUM FIX: AudioTrack.stop() can throw if not in started state
        try {
            audioTrack?.stop()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "AudioTrack.stop failed: ${e.message}")
        }
        audioTrack?.release()
        audioTrack = null

        stopSco()
        Log.d(TAG, "Audio capture stopped")
    }

    // ── Bluetooth SCO management ────────────────────────────────────

    private fun startSco() {
        // Fix 4: Guard against double-registration
        if (scoReceiver != null) {
            Log.w(TAG, "SCO receiver already registered, skipping")
            return
        }

        // Register receiver to track SCO state
        scoReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val state = intent?.getIntExtra(
                    AudioManager.EXTRA_SCO_AUDIO_STATE,
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                )
                scoConnected = (state == AudioManager.SCO_AUDIO_STATE_CONNECTED)
                Log.d(TAG, "SCO state changed: connected=$scoConnected")
                if (scoConnected) {
                    Log.i(TAG, "SCO connected — audio now routed through glasses")
                }
            }
        }
        context.registerReceiver(
            scoReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
        )

        // Route audio through Bluetooth SCO (glasses)
        @Suppress("DEPRECATION")
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        @Suppress("DEPRECATION")
        audioManager.startBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = true
        Log.d(TAG, "Bluetooth SCO requested")
    }

    private fun stopSco() {
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()
        @Suppress("DEPRECATION")
        audioManager.isBluetoothScoOn = false
        audioManager.mode = AudioManager.MODE_NORMAL

        // Fix 4: Null receiver after unregister
        scoReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        scoReceiver = null
        scoConnected = false
        Log.d(TAG, "Bluetooth SCO stopped")
    }
}
