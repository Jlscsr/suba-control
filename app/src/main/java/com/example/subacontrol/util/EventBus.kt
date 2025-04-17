package com.example.subacontrol.util

import com.example.subacontrol.util.events.CursorMoveEvent
import com.example.subacontrol.util.events.TapEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private fun publish(event: Any) {
        _events.tryEmit(event)
    }

    fun publishCursorMove(x: Int, y: Int) {
        publish(CursorMoveEvent(x, y))
    }
    fun publishTap(x: Int, y: Int) {
        publish(TapEvent(x, y))
    }
}
