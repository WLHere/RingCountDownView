package com.bwl.ringcountdownview

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<RingCountDownView>(R.id.countDown).start()
        findViewById<RingCountDownView>(R.id.countDown).setListener(object : RingCountDownView.CountDownListener {
            override fun onFinished() {
                Toast.makeText(applicationContext, "finished", Toast.LENGTH_SHORT).show()
            }
        })
    }
}