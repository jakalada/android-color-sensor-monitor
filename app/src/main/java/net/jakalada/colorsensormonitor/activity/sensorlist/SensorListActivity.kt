package net.jakalada.colorsensormonitor.activity.sensorlist

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_sensor_list.*
import kotlinx.android.synthetic.main.list_sensor_item.view.*
import net.jakalada.colorsensormonitor.R

class SensorListActivity : AppCompatActivity() {

    private lateinit var registeredAdapter: SensorListAdapter
    private lateinit var unregisteredAdapter: SensorListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_list)

        initRegisteredSensorList();
        initUnregisteredSensorList();
    }

    /** 「登録済みのセンサー」のリストの初期化処理 */
    private fun initRegisteredSensorList() {
        // RecycleViewのアダプターを作成
        registeredAdapter = SensorListAdapter(this, {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        // 仮データを追加
        registeredAdapter.addSensorName("0001")
        registeredAdapter.addSensorName("0003")

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
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        })

        // 仮データを追加
        unregisteredAdapter.addSensorName("0002")
        unregisteredAdapter.addSensorName("0004")

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

        registeredSensorList.postDelayed({
            registeredAdapter.addSensorName("0002")
            unregisteredAdapter.removeSensorName("0002")
        }, 5000)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }
}

class SensorListAdapter(context: Context, private val onItemClicked : (String) -> Unit) :
        RecyclerView.Adapter<SensorListAdapter.SensorListViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    private val sensorList = mutableListOf<String>()

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

    class SensorListViewHolder(view : View) : RecyclerView.ViewHolder(view) {
        val sensorNameTextView = view.sensorNameTextView
    }

    /**
     * センサー名の削除
     *
     * @param sensorName 削除するセンサー名(アドバタイズパケットのデバイス名)
     */
    fun removeSensorName(sensorName : String) {
        val pos = sensorList.indexOf(sensorName)
        if (pos != -1) {
            sensorList.remove(sensorName)
            notifyItemRemoved(pos)
        }
    }

    /**
     * センサー名の追加
     *
     * @param sensorName 追加するセンサー名(アドバタイズパケットのデバイス名)
     */
    fun addSensorName(sensorName : String) {
        if (!sensorList.contains(sensorName)) {
            sensorList.add(0, sensorName)
            notifyItemInserted(0)
        }
    }
}
