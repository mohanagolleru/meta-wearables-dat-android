package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit
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
  // Shared OkHttpClient with explicit timeouts.
  // Shutdown in destroy() to release thread pool + connection pool.
  private val client = OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .readTimeout(0, TimeUnit.SECONDS)    // WebSocket: indefinite read
      .writeTimeout(5, TimeUnit.SECONDS)
      .pingInterval(15, TimeUnit.SECONDS)  // keepalive — detect dead connections
      .build()

  // CRITICAL FIX: @Volatile ensures visibility across OkHttp reader/writer threads
  @Volatile private var ws: WebSocket? = null
  private val isOpen = AtomicBoolean(false)
  private val isConnecting = AtomicBoolean(false)
  private val isDestroyed = AtomicBoolean(false)

  /** Called when response audio arrives from the Mac (Gemini playback). */
  @Volatile var onAudioReceived: ((ByteArray) -> Unit)? = null

  fun connect() {
    // Skip if destroyed, already open, or if a connection attempt is already in-flight
    if (isDestroyed.get()) return
    if (isOpen.get() || !isConnecting.compareAndSet(false, true)) return
    val req = Request.Builder().url(wsUrl).build()
    ws = client.newWebSocket(req, object : WebSocketListener() {
      override fun onOpen(webSocket: WebSocket, response: Response) {
        // Guard: if close() ran while handshake was in-flight, ignore this callback
        if (ws !== webSocket) return
        isOpen.set(true)
        isConnecting.set(false)
        Log.i(TAG, "WS open url=$wsUrl")
      }

      override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val data = bytes.toByteArray()
        if (data.isNotEmpty() && data[0] == 'A'.code.toByte()) {
          // Audio response from Mac — strip prefix, deliver to bridge.
          // Snapshot callback ref to avoid calling on a nulled reference.
          val callback = onAudioReceived ?: return
          val audio = data.copyOfRange(1, data.size)
          callback.invoke(audio)
        }
      }

      override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        if (ws !== webSocket) return
        isOpen.set(false)
        isConnecting.set(false)
        Log.w(TAG, "WS closed code=$code reason=$reason")
      }

      override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        if (ws !== webSocket) return
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
    // HIGH FIX: avoid spread operator (*msg) which boxes every byte into a Byte object.
    // ByteString.of(byte[], offset, count) is zero-copy.
    val msg = ByteArray(1 + jpeg.size)
    msg[0] = 'V'.code.toByte()
    System.arraycopy(jpeg, 0, msg, 1, jpeg.size)
    val sent = ws?.send(ByteString.of(msg, 0, msg.size)) ?: false
    if (!sent) {
      isOpen.set(false)
    }
    return sent
  }

  /** Send mic audio captured from the glasses to the Mac. */
  fun sendAudio(pcmData: ByteArray): Boolean {
    if (!isOpen.get()) return false
    // HIGH FIX: avoid spread operator — use ByteString.of(byte[], offset, count)
    val msg = ByteArray(1 + pcmData.size)
    msg[0] = 'A'.code.toByte()
    System.arraycopy(pcmData, 0, msg, 1, pcmData.size)
    return ws?.send(ByteString.of(msg, 0, msg.size)) ?: false
  }

  /**
   * Close the current WebSocket connection. The OkHttpClient remains alive so that
   * subsequent connect() calls can reuse it (StreamViewModel survives navigation).
   * Call [destroy] when the ViewModel is cleared to release the thread pool.
   */
  fun close() {
    onAudioReceived = null   // prevent late callbacks to released AudioTrack
    try { ws?.close(1000, "bye") } catch (_: Exception) {}
    ws = null
    isOpen.set(false)
    isConnecting.set(false)
  }

  /**
   * Permanently shut down the OkHttpClient thread pool + connection pool.
   * Call this ONLY from StreamViewModel.onCleared() — after this, connect() will fail.
   */
  fun destroy() {
    isDestroyed.set(true)
    close()
    client.dispatcher.executorService.shutdown()
    client.connectionPool.evictAll()
  }

  companion object {
    private const val TAG = "WsBinarySender"
  }
}
