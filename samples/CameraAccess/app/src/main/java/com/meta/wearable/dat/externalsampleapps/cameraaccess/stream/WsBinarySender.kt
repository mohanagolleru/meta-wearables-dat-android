package com.meta.wearable.dat.externalsampleapps.cameraaccess.stream

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean

class WsBinarySender(private val wsUrl: String) {
  private val client = OkHttpClient()
  private var ws: WebSocket? = null
  private val isOpen = AtomicBoolean(false)
  private val isConnecting = AtomicBoolean(false)

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
    return ws?.send(ByteString.of(*msg)) ?: false
  }

  fun close() {
    try { ws?.close(1000, "bye") } catch (_: Exception) {}
    isOpen.set(false)
    isConnecting.set(false)
  }

  companion object {
    private const val TAG = "WsBinarySender"
  }
}
