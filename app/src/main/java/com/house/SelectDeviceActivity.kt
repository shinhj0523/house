package com.house

import android.Manifest

import androidx.appcompat.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle

import androidx.core.app.ActivityCompat
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import java.io.IOException
import java.lang.Exception
import java.util.*
import kotlin.Throws

class SelectDeviceActivity : AppCompatActivity() {
    private var TAG = "SelectDeviceActivity"
    var BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier
    var textStatus: TextView? = null
    private var btnParied: Button? = null
    var btnSearch: Button? = null
    var btnSend: Button? = null
    var listView: ListView? = null
    var btAdapter: BluetoothAdapter? = null
    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String>? = null
    var deviceAddressArray: ArrayList<String>? = null
    var btSocket: BluetoothSocket? = null
    var connectedThread: ConnectedThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get permission
        val permission_list = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this@SelectDeviceActivity, permission_list, 1)

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
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
            startActivityForResult(enableBtIntent, SelectDeviceActivity.Companion.REQUEST_ENABLE_BT)
        }

        // variables
        textStatus = findViewById<View>(R.id.text_status) as TextView
        btnParied = findViewById<View>(R.id.btn_paired) as Button
        btnSearch = findViewById<View>(R.id.btn_search) as Button
        btnSend = findViewById<View>(R.id.btn_send) as Button
        listView = findViewById<View>(R.id.listview) as ListView

        // Show paired devices
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        listView!!.adapter = btArrayAdapter
        listView!!.onItemClickListener = SelectDeviceActivity.myOnItemClickListener()
    }

    fun onClickButtonPaired(view: View?) {
        btArrayAdapter!!.clear()
        if (deviceAddressArray != null && !deviceAddressArray!!.isEmpty()) {
            deviceAddressArray!!.clear()
        }
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
        pairedDevices = btAdapter!!.bondedDevices
        if (pairedDevices.size > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                btArrayAdapter!!.add(deviceName)
                deviceAddressArray!!.add(deviceHardwareAddress)
            }
        }
    }

    fun onClickButtonSearch(view: View?) {
        // Check if the device is already discovering
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
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
        if (btAdapter!!.isDiscovering) {
            btAdapter!!.cancelDiscovery()
        } else {
            if (btAdapter!!.isEnabled) {
                btAdapter!!.startDiscovery()
                btArrayAdapter!!.clear()
                if (deviceAddressArray != null && !deviceAddressArray!!.isEmpty()) {
                    deviceAddressArray!!.clear()
                }
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)
            } else {
                Toast.makeText(applicationContext, "bluetooth not on", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Send string "a"
    fun onClickButtonSend(view: View?) {
        if (connectedThread != null) {
            connectedThread!!.write("a")
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                btArrayAdapter!!.add(deviceName)
                deviceAddressArray!!.add(deviceHardwareAddress)
                btArrayAdapter!!.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    inner class myOnItemClickListener : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            Toast.makeText(
                applicationContext,
                btArrayAdapter!!.getItem(position),
                Toast.LENGTH_SHORT
            ).show()
            textStatus!!.text = "try..."
            val name = btArrayAdapter!!.getItem(position) // get name
            val address = deviceAddressArray!![position] // get address
            var flag = true
            val device = btAdapter!!.getRemoteDevice(address)

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device)
                btSocket!!.connect()
            } catch (e: IOException) {
                flag = false
                textStatus!!.text = "connection failed!"
                e.printStackTrace()
            }

            // start bluetooth communication
            if (flag) {
                textStatus!!.text = "connected to $name"
                connectedThread = ConnectedThread(btSocket!!)
                connectedThread!!.start()
            }
        }
    }

    @Throws(IOException::class)
    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        try {
            val m = device.javaClass.getMethod(
                "createInsecureRfcommSocketToServiceRecord",
                UUID::class.java
            )
            return m.invoke(device, BT_MODULE_UUID) as BluetoothSocket
        } catch (e: Exception) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e)
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }
}