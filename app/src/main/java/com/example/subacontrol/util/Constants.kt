package com.example.subacontrol.util

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

object Constants {
    // Use hardcoded WebSocket URL since R can't be resolved yet
    fun getWebSocketUrl(context: Context): String {
        // Normally we would get this from resources but we'll hardcode for now
        val host = getHost() // Make sure this matches your actual IP
        return "ws://$host:${getPort()}"
    }
    
    fun getHost(): String {
        // Hardcoded IP address - MUST be changed to match the actual network
        return "192.168.18.120"
    }
    
    fun getPort(): Int {
        return 8080
    }
    
    // Try to get the WiFi IP address - for diagnostic purposes
    fun getLocalIpAddress(context: Context): String? {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ipAddress = wifiManager.connectionInfo.ipAddress
            
            // Convert little-endian to big-endian if needed and convert to IPv4 format
            @Suppress("DEPRECATION")
            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff, 
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            Log.e("Constants", "Failed to get IP address", e)
            return null
        }
    }
    
    const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
}
