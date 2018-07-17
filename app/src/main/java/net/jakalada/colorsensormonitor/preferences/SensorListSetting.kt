package net.jakalada.colorsensormonitor.preferences

import android.content.Context
import android.content.SharedPreferences

/** センサーの登録設定のクラス */
class SensorListSetting(context: Context) {

    companion object {
        /** SensorListSettingクラスで管理している設定を保存するSharePreferencesの名前 */
        private const val PREFS_NAME = "sensor_list"

        /** 登録済みのセンサーのリストのプリファレンスキー */
        private const val PREF_KEY_SENSOR_LIST = "sensor_name_list"
    }

    private val prefs: SharedPreferences =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 登録済みのセンサーのリスト */
    val list: List<String>
        get() = prefs.getStringSet(PREF_KEY_SENSOR_LIST, mutableSetOf<String>()).toList()

    /**
     * センサーを登録。
     *
     * @param sensorName センサー名
     */
    fun addSensor(sensorName: String) {
        val newList = list.toMutableList()
        newList.add(sensorName)
        prefs.edit().putStringSet(PREF_KEY_SENSOR_LIST, newList.toSet()).apply()
    }

    /**
     * センサーの登録を解除。
     *
     * @param sensorName センサー名
     */
    fun removeSensor(sensorName: String) {
        val newList = list.toMutableList()
        newList.remove(sensorName)
        prefs.edit().putStringSet(PREF_KEY_SENSOR_LIST, newList.toSet()).apply()
    }
}