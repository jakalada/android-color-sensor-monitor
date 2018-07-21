package net.jakalada.colorsensormonitor.ble

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.graphics.Color
import android.os.ParcelUuid
import java.lang.ref.WeakReference


/** カラーセンサーの通信イベントのインターフェース */
interface ColorSensorCallback {

    /** 接続状態が変わったとき */
    fun onConnectionStateChanged(deviceName: String, state: ColorSensorConnectionState)

    /** 値を受信したとき */
    fun onValueReceived(deviceName: String, value: ColorSensorValue)

    /** エラーが発生したとき */
    fun onError(deviceName: String, error: ColorSensorError)
}

/** [ColorSensorCallback.onConnectionStateChanged]で通知される通信状態 */
enum class ColorSensorConnectionState {

    /** 接続完了後 */
    CONNECTED,

    /** 接続完了後の各種セットアップ完了後 */
    SETUP_FINISHED,

    /** 切断後 */
    DISCONNECTED,
}

/** [ColorSensorCallback.onError]で通知されるエラー */
enum class ColorSensorError {

    /**
     * [BluetoothDevice.connectGatt]の結果がfalseだった場合
     */
    CONNECT_GATT_ERROR,

    /**
     * [BluetoothGattCallback.onConnectionStateChange]のstatus引数が[BluetoothGatt.GATT_SUCCESS]
     * 以外だった場合。
     */
    ON_CONNECTION_STATE_CHANGED_ERROR,

    /**
     * [BluetoothGattCallback.onServicesDiscovered]のstatus引数が[BluetoothGatt.GATT_SUCCESS]以外
     * だった場合。
     */
    ON_SERVICE_DISCOVERED_ERROR,

    /**
     * [BluetoothGattCallback.onDescriptorWrite]のstatus引数が[BluetoothGatt.GATT_SUCCESS]以外だった
     * 場合。
     */
    ON_DESCRIPTOR_WRITE_ERROR,

    /**
     * [BluetoothGatt.discoverServices]の実行結果がfalseだった場合。
     */
    DISCOVER_SERVICES_ERROR,

    /** カラーセンサーのCharacteristicのNotificationを有効化する処理に失敗した場合 */
    ENABLE_NOTIFICATION_ERROR,

    /**
     * [BluetoothGattCallback.onServicesDiscovered]の処理時にカラーセンサーに実装されているはずのService
     * が取得できなかった場合。
     */
    SERVICE_NULL_ERROR,

    /**
     * [BluetoothGattCallback.onServicesDiscovered]の処理時にカラーセンサーに実装されているはずの
     * Characteristicが取得できなかった場合。
     */
    CHARACTERISTIC_NULL_ERROR,

    /** カラーセンサーから受信したRGB値が不正だった場合 */
    INVALID_VALUE_ERROR,

    /**
     * APIが正常に動作していないと思われる致命的なエラー。
     * 例えば、 [BluetoothGattCallback.onConnectionStateChange] などのgatt引数がnullだった場合。
     */
    FATAL_ERROR
}

/** カラーセンサーから受信するデータの値クラス */
class ColorSensorValue(val value: ByteArray) {

    companion object {

        /** カラーセンサーから受信するデータのバイトサイズ */
        const val VALUE_SIZE: Int = 3

        /** カラーセンサーから受信したバイトデータから[ColorSensorValue]を生成するファクトリーメソッド */
        fun newInstance(value: ByteArray): ColorSensorValue? {
            if (value.size != VALUE_SIZE) return null

            return ColorSensorValue(value)
        }

        private val hexArray = "0123456789ABCDEF".toCharArray()

        fun toHexString(bytes: ByteArray): String {
            val hexChars = CharArray(bytes.size * 2)
            for (j in 0 until bytes.size) {
                val v = bytes[j].toInt() and 0xFF
                hexChars[j * 2] = hexArray[v.ushr(4)]
                hexChars[j * 2 + 1] = hexArray[v and 0x0F]
            }
            return String(hexChars)
        }
    }

    /** ビューの色のプロパティに設定する想定のInt値で取得 */
    val rgb: Int
        get() = Color.rgb((value[0].toInt() and 0xFF),
                (value[1].toInt() and 0xFF),
                (value[2].toInt() and 0xFF))

    /** 3バイトの各色の値を6桁のHEX文字列で取得 */
    val rgbHexString: String
        get() = toHexString(value)
}

/** カラーセンサーの通信クラス */
class ColorSensorCentral(context: Context, val deviceName: String, val callback: ColorSensorCallback) {

    /** [ColorSensorCentral]の内部状態 */
    private enum class ColorSensorCentralState {

        /** 初期状態 */
        CLOSED,

        /** [android.bluetooth.le.BluetoothLeScanner.startScan]の実行中 */
        SCANNING,

        /** [BluetoothDevice.connectGatt]の実行後 */
        CONNECTING,

        /** [BluetoothGattCallback.onConnectionStateChange]で[BluetoothGatt.STATE_CONNECTED]になって以降 */
        CONNECTED,
    }

    private val weakContext: WeakReference<Context> = WeakReference(context)
    private val weakCallback: WeakReference<ColorSensorCallback> = WeakReference(callback)

    private val lockObject = Object()
    private val gattCallback: ColorSensorGattCallback = ColorSensorGattCallback()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var state: ColorSensorCentralState = ColorSensorCentralState.CLOSED

    /** 接続する */
    fun connect(): Boolean {
        synchronized(lockObject) {
            when (state) {
                ColorSensorCentralState.CLOSED -> {
                    startScan()
                    state = ColorSensorCentralState.SCANNING
                    return true
                }

                ColorSensorCentralState.SCANNING,
                ColorSensorCentralState.CONNECTING,
                ColorSensorCentralState.CONNECTED -> return false
            }
        }
    }

    /** 切断と破棄処理 */
    fun close() {
        synchronized(lockObject) {
            stopScan()
            val gatt = this.gatt
            if (gatt != null) {
                gatt.close()
                this.gatt = null
            }
            state = ColorSensorCentralState.CLOSED
        }
    }

    private fun startScan() {
        // アドバタイズされているServiceのUUIDとデバイス名で絞り込んでスキャン
        val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(ColorSensorPeripheral.SERVICE_UUID))
                .setDeviceName(deviceName)
                .build()
        // すぐに接続したいので高精度でスキャン
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()

        bluetoothLeScanner.startScan(mutableListOf(filter), settings, colorSensorScanCallback)
    }

    private fun stopScan() {
        bluetoothLeScanner.stopScan(colorSensorScanCallback)
    }

    private fun _connect(device: BluetoothDevice) {
        synchronized(lockObject) {
            if (state == ColorSensorCentralState.SCANNING) {
                val context = weakContext.get()
                if (context != null) {
                    val gatt = device.connectGatt(context, false, gattCallback)
                    if (gatt != null) {
                        this.gatt = gatt
                        state = ColorSensorCentralState.CONNECTING
                    } else {
                        emitErrorEvent(ColorSensorError.CONNECT_GATT_ERROR)
                        return
                    }
                }
            }
        }
    }

    private fun emitConnectionStateChangedEvent(state: ColorSensorConnectionState) {
        when (state) {
            ColorSensorConnectionState.CONNECTED -> this.state = ColorSensorCentralState.CONNECTED
            ColorSensorConnectionState.SETUP_FINISHED -> {}
            ColorSensorConnectionState.DISCONNECTED -> {
                close()
                this.state = ColorSensorCentralState.CLOSED
            }
        }

        weakCallback.get()?.onConnectionStateChanged(deviceName, state)
    }

    private fun emitValueReceivedEvent(value: ColorSensorValue) {
        weakCallback.get()?.onValueReceived(deviceName, value)
    }

    private fun emitErrorEvent(error: ColorSensorError) {
        close()
        state = ColorSensorCentralState.CLOSED
        weakCallback.get()?.onError(deviceName, error)
    }

    private val colorSensorScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                if (result.scanRecord.deviceName == deviceName) {
                    stopScan()
                    _connect(result.device)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
        }

        override fun onScanFailed(errorCode: Int) {
        }
    }

    private inner class ColorSensorGattCallback : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitErrorEvent(ColorSensorError.ON_CONNECTION_STATE_CHANGED_ERROR)
                return
            }

            if (gatt == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    emitConnectionStateChangedEvent(ColorSensorConnectionState.CONNECTED)
                    if (!gatt.discoverServices()) {
                        emitErrorEvent(ColorSensorError.DISCOVER_SERVICES_ERROR)
                    }
                }

                BluetoothGatt.STATE_DISCONNECTED -> {
                    emitConnectionStateChangedEvent(ColorSensorConnectionState.DISCONNECTED)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitErrorEvent(ColorSensorError.ON_SERVICE_DISCOVERED_ERROR)
                return
            }

            if (gatt == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            // Serviceの取得
            val service = gatt.getService(ColorSensorPeripheral.SERVICE_UUID)
            if (service == null) {
                emitErrorEvent(ColorSensorError.SERVICE_NULL_ERROR)
                return
            }

            // Characteristicの取得
            val characteristic = service.getCharacteristic(ColorSensorPeripheral.CHARACTERISTIC_UUID)
            if (characteristic == null) {
                emitErrorEvent(ColorSensorError.CHARACTERISTIC_NULL_ERROR)
                return
            }

            // CharacteristicのNotificationを有効化
            if (!enableNotification(gatt, characteristic)) {
                emitErrorEvent(ColorSensorError.ENABLE_NOTIFICATION_ERROR)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                emitErrorEvent(ColorSensorError.ON_DESCRIPTOR_WRITE_ERROR)
                return
            }

            if (gatt == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            if (descriptor == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            if (descriptor.characteristic.uuid == ColorSensorPeripheral.CHARACTERISTIC_UUID) {
                emitConnectionStateChangedEvent(ColorSensorConnectionState.SETUP_FINISHED)
            } else {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (gatt == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            if (characteristic == null) {
                emitErrorEvent(ColorSensorError.FATAL_ERROR)
                return
            }

            if (characteristic.uuid == ColorSensorPeripheral.CHARACTERISTIC_UUID) {
                val colorSensorValue = ColorSensorValue.newInstance((characteristic.value))
                if (colorSensorValue != null) {
                    emitValueReceivedEvent(colorSensorValue)
                } else {
                    emitErrorEvent(ColorSensorError.INVALID_VALUE_ERROR)
                }
            }
        }

        /**
         * CharecteristicのNotificationを有効化する。
         */
        private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): Boolean {
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                return false
            }

            val descriptor = characteristic.getDescriptor(ColorSensorPeripheral.CLIENT_CHARACTERISTIC_CONFIG)
            if (descriptor == null) {
                return false
            }

            if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                return false
            }

            return gatt.writeDescriptor(descriptor)
        }
    }
}
