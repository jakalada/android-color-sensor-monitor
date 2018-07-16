package net.jakalada.colorsensormonitor.ble

import java.util.*

/** カラーセンサーのクラス */
class ColorSensor {

    companion object {

        /** ServiceのUUID */
        val SERVICE_UUID = UUID.fromString("9D86A3DA-467C-4224-B96C-36D5F85C1725")

        /** CharacteristicのUUID */
        val CHARACTERISTIC_UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A8")
    }
}