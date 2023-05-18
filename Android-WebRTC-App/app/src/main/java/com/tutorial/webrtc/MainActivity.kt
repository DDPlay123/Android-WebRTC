package com.tutorial.webrtc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tutorial.webrtc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setListener()
    }

    private fun setListener() {
        binding.apply {
            btnJoin.setOnClickListener {
                startActivity(Intent(this@MainActivity, RoomActivity::class.java))
            }
        }
    }
}