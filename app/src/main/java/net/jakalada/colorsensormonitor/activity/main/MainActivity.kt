package net.jakalada.colorsensormonitor.activity.main

import android.content.Intent
import android.hardware.Sensor
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import net.jakalada.colorsensormonitor.R
import net.jakalada.colorsensormonitor.activity.sensorlist.SensorListActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorListButton.setOnClickListener {
            val intent = Intent(this, SensorListActivity::class.java)
            startActivity(intent)
        }
    }
}