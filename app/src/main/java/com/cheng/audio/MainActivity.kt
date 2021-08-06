package com.cheng.audio

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.cheng.audio.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        val view = viewBinding.root
        setContentView(view)
        viewBinding.tvHello.text = "Hello World!"
        viewBinding.btnTest.setOnClickListener {
            start(MediaPlayerActivity::class.java)
        }
    }
}