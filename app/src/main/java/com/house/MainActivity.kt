package com.house


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.house.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


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


        binding.buttonRecognizeGesture.setOnClickListener {
            val intent = Intent(this, RecognizeGesture::class.java)
            startActivity(intent)
        }

        binding.buttonSystemSecurity.setOnClickListener {
            val intent = Intent(this, SystemSecurity::class.java)
            startActivity(intent)
        }
        binding.buttonConnectBlooth.setOnClickListener {
            val intent = Intent(this,SelectDeviceActivity::class.java)
            startActivity(intent)
        }

    }


}