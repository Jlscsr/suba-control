package com.example.subacontrol.websocket

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.example.subacontrol.util.Constants
import com.example.subacontrol.util.EventBus

class WebSocketReceiver {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
        val request = Request.Builder()
            .url(Constants.WS_URL)
            .build()

        client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, resp: Response) {
                Log.d("WebSocket", "Connected ✅")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val data = JSONObject(text)
                    val type = data.getString("type")
                    val laptopX = data.getInt("x")
                    val laptopY = data.getInt("y")

                    // screen ratios (phone vs laptop)
                    val scaleX = 1080.0 / 1920.0
                    val scaleY = 2460.0 / 1080.0
                    val x = (laptopX * scaleX).toInt()
                    val y = (laptopY * scaleY).toInt()

                    when(type) {
                        "move" -> EventBus.publishCursorMove(x, y)
                        "click" -> EventBus.publishTap(x, y)
                        else -> Log.w("WebSocket", "unknown type $type")
                    }
                } catch(e: Exception) {
                    Log.e("WebSocket", "parse fail: $text", e)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, resp: Response?) {
                Log.e("WebSocket", "Connection failed", t)
            }
        })
    }
}
