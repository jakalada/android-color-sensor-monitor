package net.jakalada.colorsensormonitor.ble

import java.util.*

/** カラーセンサーのBLE通信のクラス */
class ColorSensorPeripheral {

    companion object {

        /** ServiceのUUID */
        val SERVICE_UUID: UUID = UUID.fromString("9D86A3DA-467C-4224-B96C-36D5F85C1725")

        /** CharacteristicのUUID */
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A8")

        /**
         * CharacteristicのDescriptorのUUID。
         *
         * 参考:
         * 「GATT のはなし２｜Wireless・のおと｜サイレックス・テクノロジー株式会社」
         * http://www.silex.jp/blog/wireless/2017/06/gatt-1.html
         */
        val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}