package net.jakalada.colorsensormonitor.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_color_sensor.*
import kotlinx.android.synthetic.main.fragment_color_sensor.view.*
import net.jakalada.colorsensormonitor.R
import net.jakalada.colorsensormonitor.ble.*

private const val ARG_DEVICE_NAME = "device_name"
private const val ARG_DEVICE_INDEX = "device_index"

/**
 * カラーセンサーとの通信と受信したデータの表示を行うフラグメント。
 *
 * このフラグメントを含むアクティビティはインタラクションのイベントを制御するために
 * [ColorSensorFragment.OnFragmentInteractionListener] インターフェースを実装する必要があります。
 * to handle interaction events.
 * このフラグメントのインスタンスを生成するときは [ColorSensorFragment.newInstance] を使用してください。
 */
class ColorSensorFragment : Fragment() {

    companion object {

        /**
         * [Log.i]によるデバッグ出力のフラグ。[Log.d]を使用したいが、
         * 開発機器のMediaPad T3の仕様でDebugレベルのログが出力されないので[Log.i]を使用している。
         */
        private const val DEBUG: Boolean = true

        /** [Fragment.onResume]で開始するセンサーへの接続試行の遅延時間 */
        private const val CONNECITON_DELAY: Long = 5000L

        /** [ColorSensorFragment]で開始する接続試行の間隔 */
        private const val CONNECTION_INTERVAL: Long = 5000L

        /**
         * 指定されたパラメータでこのフラグメントのインスタンスを生成するために
         * このファクトリーメソッドを使用してください。
         *
         * @param deviceName 通信するカラーセンサー
         * @return ColorSensorFragment
         */
        @JvmStatic
        fun newInstance(deviceName: String, deviceIndex: Int) =
                ColorSensorFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_DEVICE_NAME, deviceName)
                        putInt(ARG_DEVICE_INDEX, deviceIndex)
                    }
                }
    }

    private val handler: Handler = Handler()

    /** このフラグメントが通信するカラーセンサーのデバイス名 */
    private lateinit var deviceName: String

    /** このフラグメントが通信するカラーセンサーの表示上のインデックス */
    private var deviceIndex: Int = 0

    /** カラーセンサーの通信オブジェクト */
    private lateinit var colorSensorCentral: ColorSensorCentral

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceName = it.getString(ARG_DEVICE_NAME)
            deviceIndex = it.getInt(ARG_DEVICE_INDEX)
            colorSensorCentral = ColorSensorCentral(activity!!, deviceName, colorSensorCallback)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_color_sensor, container, false)

        // デバイス名は常に表示
        view.deviceNameTextView.text = deviceName
        return view
    }

    override fun onResume() {
        super.onResume()

        // 一応タスクを空にする
        handler.removeCallbacksAndMessages(null)

        // 各ビューを初期表示に変更
        initView()

        // ほかのColorSensorFragmentが行う接続試行と重なると不安定になるので間隔をおく。
        // アドバタイズパケットを受信してから接続するため、重なることを確実に防げるわけではない。
        handler.postDelayed({
            colorSensorCentral.connect()
        }, CONNECITON_DELAY + CONNECTION_INTERVAL * deviceIndex)
    }

    override fun onPause() {
        super.onPause()

        // 強制的に切断
        colorSensorCentral.close()

        // タスクを空にする
        handler.removeCallbacksAndMessages(null)
    }

    /** 接続前の初期状態にビューの表示を設定 */
    private fun initView() {
        // 接続前なのでプログレスバーを表示
        connectionProgress.visibility = View.VISIBLE

        // 受信した色データのビューを非表示
        colorView.visibility = View.INVISIBLE
        colorView.setBackgroundColor(Color.WHITE)
        colorTextView.visibility = View.INVISIBLE
        colorTextView.text = ""

        // 切断時のビューを非表示
        disconnectedTextView.visibility = View.INVISIBLE

        // エラー時のビューを非表示
        errorTextView.visibility = View.INVISIBLE
        errorTextView.text = ""
    }

    /** 色データの受信のためのセットアップ完了後の状態に変更 */
    private fun changeViewOnSetupFinished() {
        // 受信した色データのビューを表示
        colorTextView.visibility = View.VISIBLE
        colorView.visibility = View.VISIBLE

        // 接続完了後なのでプログレスバーを非表示
        connectionProgress.visibility = View.INVISIBLE
    }

    /** 切断時の状態に変更*/
    private fun changeViewOnDisconnected() {
        // 切断時のビューを表示
        disconnectedTextView.visibility = View.VISIBLE

        // 色データのビューを非表示
        colorView.visibility = View.INVISIBLE
        colorTextView.visibility = View.INVISIBLE

        // プログレスバーを非表示
        connectionProgress.visibility = View.INVISIBLE
    }

    /** エラー時の状態に変更*/
    private fun changeViewOnError(error: ColorSensorError) {
        // エラーを設定してビューを表示
        errorTextView.text = error.toString()
        errorTextView.visibility = View.VISIBLE

        // 色データのビューを非表示
        colorView.visibility = View.INVISIBLE
        colorTextView.visibility = View.INVISIBLE

        // プログレスバーを非表示
        connectionProgress.visibility = View.INVISIBLE
    }

    /** 受信した色データをビューにセット */
    private fun setColor(value: ColorSensorValue) {
        colorView.setBackgroundColor(value.rgb)
        colorTextView.text = "#" + value.rgbHexString
    }

    /** カラーセンサーの通信コールバック */
    private val colorSensorCallback = object : ColorSensorCallback {

        override fun onConnectionStateChanged(deviceName: String, state: ColorSensorConnectionState) {
            if (DEBUG) Log.i("ColorSensor", "[$deviceName] state is $state")

            when (state) {
                ColorSensorConnectionState.CONNECTED -> {
                    // 接続完了時
                    // 接続完了直後の通信で失敗することが多いので、
                    // SETUP_FINISHEDでビューの変更などを行う。
                }

                ColorSensorConnectionState.SETUP_FINISHED -> {
                    // カラーセンサーから色データを受信する準備が完了したとき
                    handler.post {
                        changeViewOnSetupFinished()
                    }
                }

                ColorSensorConnectionState.DISCONNECTED -> {
                    // 切断時
                    handler.post {
                        if (isResumed) {
                            changeViewOnDisconnected()
                        }
                    }
                }
            }
        }

        override fun onValueReceived(deviceName: String, value: ColorSensorValue) {
            // 色データ受信時
            if (DEBUG) Log.i("ColorSensor", "[$deviceName] value is ${value.value[0].toInt() and 0xFF} ${value.value[1].toInt() and 0xFF} ${value.value[2].toInt() and 0xFF}")

            handler.post {
                if (isResumed) {
                    setColor(value)
                }
            }
        }

        override fun onError(deviceName: String, error: ColorSensorError) {
            // エラー発生時
            if (DEBUG) Log.i("ColorSensor", "[$deviceName] error is $error")

            handler.post {
                if (isResumed) {
                    changeViewOnError(error)
                }
            }
        }
    }
}
