package net.jakalada.colorsensormonitor.fragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_color_sensor.view.*

import net.jakalada.colorsensormonitor.R

private const val ARG_DEVICE_NAME = "device_name"

/**
 * カラーセンサーとの通信と受信したデータの表示を行うフラグメント。
 *
 * このフラグメントを含むアクティビティはインタラクションのイベントを制御するために
 * [ColorSensorFragment.OnFragmentInteractionListener] インターフェースを実装する必要があります。
 * to handle interaction events.
 * このフラグメントのインスタンスを生成するときは [ColorSensorFragment.newInstance] を使用してください。
 */
class ColorSensorFragment : Fragment() {

    /** このフラグメントが通信するカラーセンサーのデバイス名 */
    private var deviceName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            deviceName = it.getString(ARG_DEVICE_NAME)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_color_sensor, container, false)
        view.deviceNameTextView.text = deviceName
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }

    companion object {

        /**
         * 指定されたパラメータでこのフラグメントのインスタンスを生成するために
         * このファクトリーメソッドを使用してください。
         *
         * @param deviceName 通信するカラーセンサー
         * @return ColorSensorFragment
         */
        @JvmStatic
        fun newInstance(deviceName: String) =
                ColorSensorFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_DEVICE_NAME, deviceName)
                    }
                }
    }
}
