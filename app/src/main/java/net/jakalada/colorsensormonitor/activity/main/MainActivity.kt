package net.jakalada.colorsensormonitor.activity.main

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import net.jakalada.colorsensormonitor.R
import net.jakalada.colorsensormonitor.activity.sensorlist.SensorListActivity
import net.jakalada.colorsensormonitor.preferences.SensorListSetting

class MainActivity : AppCompatActivity() {

    private lateinit var sensorListSetting: SensorListSetting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        sensorListSetting = SensorListSetting(this)

        initColorSensorView()
    }

    private fun initColorSensorView() {
        if (sensorListSetting.list.isEmpty()) {
            // 登録済みのセンサーがない場合は登録を促す
            noticeTextView.visibility = View.VISIBLE
        } else {
            // 登録済みのセンサーが存在する場合はセンサーごとに項目を追加する
            noticeTextView.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.action_sensor_list_setting -> {
            val intent = Intent(this, SensorListActivity::class.java)
            startActivity(intent)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}