package com.example.subacontrol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.subacontrol.overlay.OverlayService

class MainActivity : AppCompatActivity() {

    private val overlayLauncher = registerForActivityResult(
      ActivityResultContracts.StartActivityForResult()
    ) {
      if (Settings.canDrawOverlays(this)) {
        startService(Intent(this, OverlayService::class.java))
        finish()
      }
    }

    override fun onCreate(s: Bundle?) {
      super.onCreate(s)
      if (!Settings.canDrawOverlays(this)) {
        overlayLauncher.launch(
          Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                 Uri.parse("package:$packageName"))
        )
      } else {
        startService(Intent(this, OverlayService::class.java))
        finish()
      }
    }
}
