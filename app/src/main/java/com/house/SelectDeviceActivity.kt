package com.house

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.DeviceList
import com.house.databinding.ActivitySelectDeviceBinding

class SelectDeviceActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectDeviceBinding

    private var bt: BluetoothSPP? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bt = BluetoothSPP(this) //Initializing
        if (!bt!!.isBluetoothAvailable) { //블루투스 사용 불가
            Toast.makeText(
                applicationContext, "Bluetooth is not available", Toast.LENGTH_SHORT
            ).show()
            finish()
        }
        bt!!.setOnDataReceivedListener { _, message ->
            //데이터 수신
            Toast.makeText(this@SelectDeviceActivity, message, Toast.LENGTH_SHORT).show()
        }
        bt!!.setBluetoothConnectionListener(object : BluetoothSPP.BluetoothConnectionListener {
            //연결됐을 때
            override fun onDeviceConnected(name: String, address: String) {
                Toast.makeText(
                    applicationContext, "Connected to $name\n$address", Toast.LENGTH_SHORT
                ).show()
            }

            override fun onDeviceDisconnected() { //연결해제
                Toast.makeText(
                    applicationContext, "Connection lost", Toast.LENGTH_SHORT
                ).show()
            }

            override fun onDeviceConnectionFailed() { //연결실패
                Toast.makeText(
                    applicationContext, "Unable to connect", Toast.LENGTH_SHORT
                ).show()
            }
        })
        val btnConnect: Button = findViewById(com.house.R.id.btnConnect) //연결시도
        btnConnect.setOnClickListener {
            if (bt!!.serviceState == BluetoothState.STATE_CONNECTED) {
                bt!!.disconnect()
            } else {
                val intent = Intent(applicationContext, DeviceList::class.java)
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bt!!.stopService() //블루투스 중지
    }

    override fun onStart() {
        super.onStart()
        if (!bt!!.isBluetoothEnabled) { //
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT)
        } else {
            if (!bt!!.isServiceAvailable) {
                bt!!.setupService()
                bt!!.startService(BluetoothState.DEVICE_OTHER) //DEVICE_ANDROID는 안드로이드 기기 끼리
                setup()
            }
        }
    }

    private fun setup() {
        val btnSend: Button = findViewById(com.house.R.id.btnSend) //데이터 전송
        btnSend.setOnClickListener { bt?.send("Text", true) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) bt!!.connect(data)
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt!!.setupService()
                bt!!.startService(BluetoothState.DEVICE_OTHER)
                setup()
            } else {
                Toast.makeText(
                    applicationContext, "Bluetooth was not enabled.", Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}