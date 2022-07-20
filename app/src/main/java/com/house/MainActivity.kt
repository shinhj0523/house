package com.house


import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.house.databinding.ActivityMainBinding
import java.io.IOException
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    var TAG = "MainActivity"
    var BT_MODULE_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // "random" unique identifier


    var textStatus: TextView? = null
    var btnParied: Button? = null

    var btAdapter = BluetoothAdapter.getDefaultAdapter()
    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String>? = null
    var deviceAddressArray: ArrayList<String>? = null

    private val REQUEST_ENABLE_BT = 1
    var btSocket: BluetoothSocket? = null
    var connectedThread: ConnectedThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.buttonCheckTemperature.setOnClickListener {
            val intent = Intent(this, CheckTemerature::class.java)
            startActivity(intent)
        }

        binding.buttonRecognizeVoice.setOnClickListener {
            val intent = Intent(this, RecognizeVoice::class.java)
            startActivity(intent)
        }
        binding.buttonCheckElectCharge.setOnClickListener {
            val intent = Intent(this, CheckElectCharge::class.java)
            startActivity(intent)
        }


        binding.buttonCloseBlind.setOnClickListener {
            Toast.makeText(this, "커튼이 닫힙니다.",Toast.LENGTH_SHORT)
            onClickButtonSend("closeBlind")
        }

        binding.buttonOpenBlind.setOnClickListener {
            Toast.makeText(this, "커튼이 열립니다.",Toast.LENGTH_SHORT)
            onClickButtonSend("openBlind")

        }
//        binding.buttonConnectBlooth.setOnClickListener {
//            val intent = Intent(this,SelectDeviceActivity::class.java)
//            startActivity(intent)
//        }

        // Get permission
        val permission_list = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this@MainActivity, permission_list, 1)

        // Enable bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!btAdapter.isEnabled) {
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
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // variables
        textStatus = binding.textStatus
        btnParied = binding.btnPaired



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
        pairedDevices = btAdapter.bondedDevices as MutableSet<BluetoothDevice>?
        if (pairedDevices!!.size > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (device in pairedDevices as MutableSet<BluetoothDevice>) {
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                btArrayAdapter!!.add(deviceName)
                deviceAddressArray!!.add(deviceHardwareAddress)
            }
        }
    }


    // Send string "a"
    private fun onClickButtonSend(txt: String) {
        connectedThread?.write(txt)
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
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                val deviceName = device!!.name
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

    inner class myOnItemClickListener : AdapterView.OnItemClickListener {
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
            val device = btAdapter.getRemoteDevice(address)

            // create & connect socket
            try {
                btSocket = createBluetoothSocket(device)
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
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
                btSocket!!.connect()
            } catch (e: IOException) {
                flag = false
                textStatus!!.text = "connection failed!"
                e.printStackTrace()
            }

            // start bluetooth communication
            if (flag) {
                textStatus!!.text = "connected to $name"
                connectedThread = btSocket?.let { ConnectedThread(it) }
                connectedThread?.start()
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
//            return TODO;
        }
        return device.createRfcommSocketToServiceRecord(BT_MODULE_UUID)
    }

    companion object {
        private const val REQUEST_ENABLE_BT = 1
    }




}