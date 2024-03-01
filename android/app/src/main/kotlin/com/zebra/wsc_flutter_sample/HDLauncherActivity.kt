package com.zebra.wsc_flutter_sample

import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import com.zebra.valueadd.IZVAService
import io.flutter.embedding.android.FlutterActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class HDLauncherActivity : FlutterActivity(), ServiceConnection {

    private var iServiceBinder: IZVAService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hdlauncher)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        iServiceBinder = IZVAService.Stub.asInterface(service)
        Log.i("TAG", "WSC connected")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        iServiceBinder = null
        Log.e("TAG", "WSC disconnected")
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(this)
    }

    override fun onStart() {
        super.onStart()
        bindtoZVAService()
    }

    var pkg = "com.zebra.workstationconnect.release"
    private fun bindtoZVAService() {
        val intent = Intent("com.zebra.workstationconnect.release")
        intent.setClassName(
            "com.zebra.workstationconnect.release",
            "com.zebra.workstationconnect.DeviceManagementService"
        )
        bindService(intent, this, BIND_AUTO_CREATE)
    }

    fun callProcessZVA(jsonConfig: String?) {
        try {
            if (iServiceBinder != null) {
                val dataSet: String = loadJSONFromAsset(jsonConfig)!!
                val response = iServiceBinder!!.processZVARequest(dataSet)
                Log.i("callProcessZVA", "processZVARequest response=$response")
                Toast.makeText(this, "processZVARequest response=$response", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Log.e("callProcessZVA", "res " + null)
                Toast.makeText(this, "WSC Not Connected", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "ZVA Excp \n" + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun loadJSONFromAsset(jsonAssetFileName: String?): String? {
        var json: String? = null
        json = try {
            val `is` = this.assets.open(jsonAssetFileName!!)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, charset("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    fun onClickbtn_DisplaySettings(v: View?) {
        callProcessZVA("Config_DisplaySettings.json")
    }

    fun onClickbtn_DesktopUIelems(v: View?) {
        callProcessZVA("Config_DesktopUIElements.json")
    }

    fun onClickbtn_StatusIcons(v: View?) {
        callProcessZVA("Config_StatusIcons.json")
    }

    fun onClickbtn_AppsBehaviors(v: View?) {
        callProcessZVA("Config_AppBehaviors.json")
    }

    fun onClickbtn_Taskbar(v: View?) {
        callProcessZVA("Config_Taskbar.json")
    }
    fun onClickbtn_DeviceBehaviors(v: View?) {
        callProcessZVA("Config_DeviceBehaviors.json")
    }

    fun onClickbtn_OperatorOne(v: View?) {
        callProcessZVA("Config_MANAGER.json")
    }

    fun onClickbtn_OperatorTwo(v: View?) {
        callProcessZVA("Config_DIMENSIONING.json")
    }

    fun onClickbtn_OperatorThree(v: View?) {
        sendWSCConfigViaSSM()//NOT YET IMPLEMENTED
    }


    private val AUTHORITY_FILE =
        "content://com.zebra.securestoragemanager.securecontentprovider/files/"
    private val RETRIEVE_AUTHORITY =
        "content://com.zebra.securestoragemanager.securecontentprovider/file/*"
    private val COLUMN_DATA_NAME = "data_name"
    private val COLUMN_DATA_VALUE = "data_value"
    private val COLUMN_DATA_TYPE = "data_type"
    private val COLUMN_DATA_PERSIST_REQUIRED = "data_persist_required"
    private val COLUMN_TARGET_APP_PACKAGE = "target_app_package"
    private val signature = ""
    var TAG = "sendWSCConfigViaSSM"

    fun sendWSCConfigViaSSM() {
        val tmpLocalConfigFile = "/sdcard/Download/Config.json"
        val _target_package = "com.zebra.workstationconnect.release"
        val _target_sig = ""
        val targetPath =
            "com.zebra.workstationconnect.release/enterprise/workstation_connect_config.txt"

        //LOAD ASSET
        val jsonAsset = loadJSONFromAsset("Config_MANAGER.json")

        //SAVE TO LOCAL FILE
        saveStringToLocalFile(tmpLocalConfigFile, jsonAsset)

        //FEED THE LOCAL FILE TO SSM
        FeedLocalFileToSSM(tmpLocalConfigFile, _target_package, _target_sig, targetPath)
    }

    private fun FeedLocalFileToSSM(
        srcFile: String,
        _target_package: String,
        _target_sig: String,
        targetPath: String
    ) {
        val cpUriQuery = Uri.parse(AUTHORITY_FILE + packageName)
        Log.i(TAG, "authority  $cpUriQuery")
        val _sb = StringBuilder()
        try {
            val values = ContentValues()
            val _package_sig = "{\"pkg\":\"$_target_package\",\"sig\":\"$_target_sig\"}"
            val allPackagesSigs = "{\"pkgs_sigs\":[$_package_sig]}"
            values.put(COLUMN_TARGET_APP_PACKAGE, allPackagesSigs)
            values.put(COLUMN_DATA_NAME, srcFile)
            values.put(COLUMN_DATA_TYPE, "3")
            values.put(COLUMN_DATA_VALUE, targetPath)
            values.put(COLUMN_DATA_PERSIST_REQUIRED, "false")
            val createdRow = contentResolver.insert(cpUriQuery, values)
            Log.i(TAG, "SSM Insert File: " + createdRow.toString())
            //Toast.makeText(this, "File insert success", Toast.LENGTH_SHORT).show();
            _sb.append("Insert Result rows: $createdRow\n")
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "SSM Insert File - error: " + e.message + "\n\n")
            _sb.append("SSM Insert File - error: " + e.message + "\n\n")
        }
    }

    private fun saveStringToLocalFile(srcFile: String, jsonAsset: String?) {
        try {
            val f = File(srcFile)
            if (f.exists()) {
                f.delete()
            }
            f.createNewFile()
            val _p = Runtime.getRuntime().exec("chmod 666 $srcFile") //chmod needed for /enterprise
            _p.waitFor()
            Log.i(TAG, "chmod 666 result=" + _p.exitValue())
            val fos = FileOutputStream(f)
            fos.write(jsonAsset!!.toByteArray(StandardCharsets.UTF_8))
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

}