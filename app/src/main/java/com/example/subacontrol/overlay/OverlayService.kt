package com.example.subacontrol.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import com.example.subacontrol.R
import com.example.subacontrol.accessibility.TapAccessibilityService
import com.example.subacontrol.websocket.WebSocketReceiver
import com.example.subacontrol.util.EventBus
import com.example.subacontrol.util.events.CursorMoveEvent
import com.example.subacontrol.util.events.TapEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var view: WindowManager.LayoutParams
    private lateinit var cursorView: android.view.View
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        cursorView = inflater.inflate(R.layout.cursor_overlay, null)
        view = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100; y = 100
        }
        wm.addView(cursorView, view)

        // subscribe
        EventBus.events
            .onEach { e ->
                when(e) {
                    is CursorMoveEvent -> {
                        view.x = e.x; view.y = e.y
                        wm.updateViewLayout(cursorView, view)
                    }
                    is TapEvent -> {
                        TapAccessibilityService.instance?.simulateTap(e.x, e.y)
                    }
                }
            }
            .launchIn(scope)

        // kick off websocket → EventBus
        WebSocketReceiver().connect()
    }

    override fun onDestroy() {
        wm.removeView(cursorView)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(i: Intent?) = null
}
