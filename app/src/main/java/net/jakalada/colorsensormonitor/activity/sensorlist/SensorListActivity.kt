package net.jakalada.colorsensormonitor.activity.sensorlist

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_sensor_list.*
import kotlinx.android.synthetic.main.list_sensor_item.view.*
import net.jakalada.colorsensormonitor.R
import net.jakalada.colorsensormonitor.ble.ColorSensor
import net.jakalada.colorsensormonitor.preferences.SensorListSetting

/** センサー登録画面のクラス */
class SensorListActivity : AppCompatActivity() {

    private lateinit var sensorListSetting: SensorListSetting
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var registeredAdapter: SensorListAdapter
    private lateinit var unregisteredAdapter: SensorListAdapter

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_list)

        toolbar.title = getString(R.string.activity_title_sensor_list)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sensorListSetting = SensorListSetting(this)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        initRegisteredSensorList()
        initUnregisteredSensorList()
    }

    /** 「登録済みのセンサー」のリストの初期化処理 */
    private fun initRegisteredSensorList() {
        // RecycleViewのアダプターを作成
        // SharedPreferencesに保存していたセンサー名のリストを初期値として設定
        registeredAdapter = SensorListAdapter(this, {
            unregisterSensor(it)
        }, sensorListSetting.list)

        // RecycleViewにアダプターを設定
        registeredSensorList.adapter = registeredAdapter

        // 項目を縦に表示するように設定
        registeredSensorList.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false)
    }

    /** 「周囲のセンサー」のリストの初期化処理 */
    private fun initUnregisteredSensorList() {
        // RecycleViewのアダプターを作成
        unregisteredAdapter = SensorListAdapter(this, {
            registerSensor(it)
        })

        // RecycleViewにアダプターを設定
        unregisteredSensorList.adapter = unregisteredAdapter

        // 項目を縦に表示するように設定
        unregisteredSensorList.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        if (bluetoothAdapter.isEnabled && ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startScan()
        } else {
            // 許可を求める処理はMainActivityに任せる
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScan()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun startScan() {
        // アドバタイズされているServiceのUUIDで絞り込んでスキャン
        val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ColorSensor.SERVICE_UUID))
                .build()
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()
        bluetoothLeScanner.startScan(mutableListOf(filter), settings, colorSensorScanCallback)
    }

    private fun stopScan() {
        bluetoothLeScanner.stopScan(colorSensorScanCallback)
    }

    /** 「登録済みのセンサー」の項目タッチ時の処理 */
    private fun registerSensor(sensorName: String) {
        if (registeredAdapter.contains(sensorName)) return

        // 「登録済みのセンサー」のリストに追加
        sensorListSetting.addSensor(sensorName)
        registeredAdapter.addSensorName(sensorName)

        // 「周囲のセンサー」のリストから削除
        unregisteredAdapter.removeSensorName(sensorName)
    }

    /** 「周囲のセンサー」の項目タッチ時の処理 */
    private fun unregisterSensor(sensorName: String) {
        if (!registeredAdapter.contains(sensorName)) return

        sensorListSetting.removeSensor(sensorName)
        registeredAdapter.removeSensorName(sensorName)
    }

    /** 検出したセンサーの名前を「周囲のセンサー」のリストに追加する */
    private fun addUnregisteredSensorName(sensorName: String) {
        // 登録済みのセンサーの場合は「周囲のセンサー」のリストに追加しない
        if (registeredAdapter.contains(sensorName)) return

        // 「周囲のセンサー」のリストに追加
        unregisteredAdapter.addSensorName(sensorName)
    }

    private val colorSensorScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                handler.post {
                    addUnregisteredSensorName(result.scanRecord.deviceName)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        }

        override fun onScanFailed(errorCode: Int) {
        }
    }
}

/**
 * 「登録済みのセンサー」と「周囲のセンサー」で使用するAdapter
 *
 * @param context Context
 * @param onItemClicked 項目がタッチされたときのコールバック
 * @param initialSensorList 項目の初期値のリスト
 */
class SensorListAdapter(
        context: Context,
        private val onItemClicked: (String) -> Unit,
        initialSensorList: List<String> = listOf<String>()) :
        RecyclerView.Adapter<SensorListAdapter.SensorListViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    private val sensorList = initialSensorList.toMutableList()

    override fun getItemCount(): Int {
        return sensorList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorListViewHolder {
        // Viewを生成
        val view = inflater.inflate(R.layout.list_sensor_item, parent, false)

        // ViewHolderを作成
        val viewHolder = SensorListViewHolder(view)

        // 項目をタップしたときの処理を設定
        view.setOnClickListener {
            val sensorName = sensorList[viewHolder.adapterPosition]

            // センサーの名前を指定してコールバックを実行
            onItemClicked(sensorName)
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: SensorListViewHolder, position: Int) {
        val sensorName = sensorList[position]
        holder.sensorNameTextView.text = sensorName
    }

    class SensorListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sensorNameTextView: TextView = view.sensorNameTextView
    }

    /**
     * センサー名の削除
     *
     * @param sensorName 削除するセンサー名
     */
    fun removeSensorName(sensorName: String) {
        val pos = sensorList.indexOf(sensorName)
        if (pos != -1) {
            sensorList.remove(sensorName)
            notifyItemRemoved(pos)
        }
    }

    /**
     * センサー名の追加
     *
     * @param sensorName 追加するセンサー名
     */
    fun addSensorName(sensorName: String) {
        if (sensorList.contains(sensorName)) return

        sensorList.add(sensorName)
        notifyItemInserted(sensorList.size - 1)
    }

    /**
     * 指定されたセンサー名が項目として存在するかどうかを判定
     *
     * @param sensorName 追加するセンサー名(アドバタイズパケットのデバイス名)
     * @return 存在する場合はtrue。存在しない場合はfalse
     */
    fun contains(sensorName: String): Boolean {
        return sensorList.contains(sensorName)
    }
}
