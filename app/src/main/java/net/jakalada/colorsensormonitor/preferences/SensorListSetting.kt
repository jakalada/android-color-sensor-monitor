package net.jakalada.colorsensormonitor.preferences

import android.content.Context
import android.content.SharedPreferences

class SensorListSetting(context: Context) {

    companion object {
        private const val PREFS_NAME = "sensor_list"
        private const val PREF_KEY_SENSOR_LIST = "sensor_name_list"
    }

    private val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val list: List<String>
        get() = prefs.getStringSet(PREF_KEY_SENSOR_LIST, mutableSetOf<String>()).toList()

    fun addSensor(sensorName: String) {
        val newList = list.toMutableList()
        newList.add(sensorName)
        prefs.edit().putStringSet(PREF_KEY_SENSOR_LIST, newList.toSet()).apply()
    }

    fun removeSensor(sensorName: String) {
        val newList = list.toMutableList()
        newList.remove(sensorName)
        prefs.edit().putStringSet(PREF_KEY_SENSOR_LIST, newList.toSet()).apply()
    }
}