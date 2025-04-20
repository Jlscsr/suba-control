package com.example.subacontrol.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.subacontrol.websocket.WebSocketReceiver

/**
 * Connection states for WebSocket
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Singleton manager for WebSocket connections
 * Provides a centralized way to monitor connection state and manage lifecycle
 */
object WebSocketManager {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private var webSocketReceiver: WebSocketReceiver? = null
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    // WebSocket callbacks for monitoring connection status
    private val connectionCallbacks = object : WebSocketConnectionCallbacks {
        override fun onConnecting() {
            mainScope.launch {
                _connectionState.value = ConnectionState.CONNECTING
                Log.d("WebSocketManager", "WebSocket connecting...")
            }
        }
        
        override fun onConnected() {
            mainScope.launch {
                _connectionState.value = ConnectionState.CONNECTED
                Log.d("WebSocketManager", "WebSocket connected successfully")
            }
        }
        
        override fun onDisconnected() {
            mainScope.launch {
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.d("WebSocketManager", "WebSocket disconnected")
            }
        }
        
        override fun onError(message: String) {
            mainScope.launch {
                _connectionState.value = ConnectionState.ERROR
                Log.e("WebSocketManager", "WebSocket error: $message")
            }
        }
    }
    
    /**
     * Connect to WebSocket server
     */
    fun connect(context: Context) {
        if (_connectionState.value == ConnectionState.CONNECTING || 
            _connectionState.value == ConnectionState.CONNECTED) {
            return
        }
        
        ioScope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            
            try {
                // Create new receiver if needed
                if (webSocketReceiver == null) {
                    withContext(Dispatchers.Main) {
                        webSocketReceiver = WebSocketReceiver(context, connectionCallbacks)
                    }
                }
                
                // Start connection
                webSocketReceiver?.connect()
            } catch (e: Exception) {
                Log.e("WebSocketManager", "Error connecting to WebSocket", e)
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }
    
    /**
     * Disconnect from WebSocket server
     */
    fun disconnect() {
        ioScope.launch {
            try {
                webSocketReceiver?.disconnect()
                webSocketReceiver = null
                _connectionState.value = ConnectionState.DISCONNECTED
            } catch (e: Exception) {
                Log.e("WebSocketManager", "Error disconnecting from WebSocket", e)
            }
        }
    }
}

/**
 * Interface for WebSocket connection callbacks
 */
interface WebSocketConnectionCallbacks {
    fun onConnecting()
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
} 