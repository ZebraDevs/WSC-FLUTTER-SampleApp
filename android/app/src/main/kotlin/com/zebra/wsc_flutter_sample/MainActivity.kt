package com.zebra.wsc_flutter_sample

import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.view.View
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

//https://docs.flutter.dev/platform-integration/platform-channels?tab=android-channel-kotlin-tab

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.zebra.wsc_flutter_sample/WSCMethodChannel"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
                call, result -> btClick_HDLauncher()
        }
    }

    fun btClick_HDLauncher() {
        //Launch on 2nd display if available
        val ao = ActivityOptions.makeBasic()
        var other_display_id = -1
        val cur_display_id = display!!.displayId
        if (cur_display_id > 0) {
            other_display_id = cur_display_id
        } else {
            val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
            val displays = displayManager.displays
            for (_d in displays) {
                if (_d.displayId > 0) {
                    other_display_id = _d.displayId
                    break
                }
            }
        }
        ao.setLaunchDisplayId(other_display_id)
        val bao = ao.toBundle()
        val intent = Intent(baseContext, HDLauncherActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent, bao)
    }

}

