package net.jakalada.colorsensormonitor.activity.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import net.jakalada.colorsensormonitor.R
import net.jakalada.colorsensormonitor.activity.sensorlist.SensorListActivity
import net.jakalada.colorsensormonitor.fragment.ColorSensorFragment
import net.jakalada.colorsensormonitor.preferences.SensorListSetting

/** カラーセンサーの値の受信画面 */
class MainActivity : AppCompatActivity() {

    companion object {

        /**
         * [BluetoothAdapter.ACTION_REQUEST_ENABLE]のインテントによるリクエストコード
         */
        private const val REQUEST_ENABLE_BT = 1

        /**
         * [ActivityCompat.requestPermissions]でACCESS_COARSE_LOCATION権限を求めるリクエストコード
         */
        private const val PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100
    }

    private lateinit var bluetoothAdapter : BluetoothAdapter

    /** SensorListActivityで登録されているセンサーのリスト */
    private lateinit var sensorListSetting: SensorListSetting

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        sensorListSetting = SensorListSetting(this)

        // センサーの登録状況に応じてレイアウトを変更
        initColorSensorView()
    }

    override fun onStart() {
        super.onStart()

        checkAndRequestBluetooth()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // ACCESS_COARSE_LOCATION権限が付与されていないかもしれないので再度実行
                    checkAndRequestBluetooth()
                } else {
                    Toast.makeText(this,
                            getString(R.string.notice_enable_bluetooth),
                            Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetoothが無効状態かもしれないので再度実行
                checkAndRequestBluetooth()
            } else {
                Toast.makeText(this,
                        getString(R.string.notice_access_coarse_location_permission),
                        Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initColorSensorView() {
        if (sensorListSetting.list.isEmpty()) {
            // 登録済みのセンサーがない場合は登録を促す
            noticeTextView.visibility = View.VISIBLE
        } else {
            // 登録済みのセンサーが存在する場合はセンサーごとに項目を追加する
            noticeTextView.visibility = View.GONE

            sensorListSetting.list.sorted().forEachIndexed { index: Int, deviceName: String ->
                val fragmentTransaction = supportFragmentManager.beginTransaction()
                val fragment : Fragment = ColorSensorFragment.newInstance(deviceName, index)
                fragmentTransaction.add(R.id.colorSensorFragmentLayout, fragment)
                fragmentTransaction.commit()
            }
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

    private fun checkAndRequestBluetooth() : Boolean {
        // Bluetooth機能がOFFの場合は有効化を要求
        if (!bluetoothAdapter.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT)
            return false
        }

        // ACCESS_COARSE_LOCATION権限が許可されていない場合は許可を要求
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // 明示的に不許可にされていた場合や、
                // 許可の確認ダイアログで「今後は表示しない」が選択されていた場合
                // 本来は許可を求めるべきではないが、開発中なので許可を求める
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                        PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
            }
            return false
        }

        // Bluetoothが有効、かつ、ACCESS_COARSE_LOCATION権限が許可されている場合
        return true
    }
}