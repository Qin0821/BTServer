package com.dragonfly.bt.server

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class BleActivity : AppCompatActivity() {

    companion object {
        private val TAG = BluetoothLeService::class.java.simpleName
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        const val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
        const val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

        private val UUID_ADV_SERVER = UUID.fromString("0000ffe3-0000-1000-8000-00805f9b34fb")
        private val UUID_SERVER = UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARREAD = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb")
        private val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
        private val UUID_CHARWRITE = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    }

    private lateinit var characteristicRead: BluetoothGattCharacteristic

    private var connectionState = STATE_DISCONNECTED

    private val mBluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        mBluetoothManager.adapter
    }
    private val mBluetoothManager: BluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private lateinit var bluetoothGattServer: BluetoothGattServer


    private var connected = false
    private var mGattCharacteristics = mutableListOf<BluetoothGattCharacteristic>()

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initGATTServer()
    }


    fun addTip(msg: String) {
        runOnUiThread {
            tvRead.text = "${tvRead.text}\n$msg"
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initGATTServer() {
        val mSettings = AdvertiseSettings.Builder()
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .build()

        val mAdvertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .setIncludeTxPowerLevel(true)
            .addServiceUuid(ParcelUuid(UUID_ADV_SERVER))
            .build()

        //        .addServiceUuid(new ParcelUuid(UUID_SERVER))

        //        .addServiceUuid(new ParcelUuid(UUID_SERVER))
        val  mScanResponseData =
            AdvertiseData.Builder() //        .addServiceUuid(new ParcelUuid(UUID_ADV_SERVER))
                .setIncludeTxPowerLevel(true)
                .addServiceData(ParcelUuid(UUID_SERVER), byteArrayOf(0, 2, 3))
                .build()

        mBluetoothAdapter.name = "Ble Server"
        val mCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                addTip("BLE advertisement added successfully")
                initServices()
            }

            override fun onStartFailure(errorCode: Int) {
                addTip("Failed to add BLE advertisement, reason: $errorCode")
            }
        }

        val mBluetoothLeAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser
        mBluetoothLeAdvertiser.startAdvertising(
            mSettings,
            mAdvertiseData,
            mScanResponseData,
            mCallback
        )

    }

    private fun initServices() {

        bluetoothGattServer = mBluetoothManager.openGattServer(this, bluetoothGattServerCallback);
        val service = BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        //add a read characteristic.
        characteristicRead = BluetoothGattCharacteristic(
            UUID_CHARREAD,
            BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        //add a descriptor
        val descriptor = BluetoothGattDescriptor(
            UUID_DESCRIPTOR,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        characteristicRead.addDescriptor(descriptor)
        service.addCharacteristic(characteristicRead)

        //add a write characteristic.
        val characteristicWrite = BluetoothGattCharacteristic(
            UUID_CHARWRITE,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristicWrite)

        bluetoothGattServer.addService(service)
        addTip("2. initServices ok")
    }

    /**
     * 服务事件的回调
     */
    private val bluetoothGattServerCallback: BluetoothGattServerCallback =
        object : BluetoothGattServerCallback() {
            /**
             * 1.连接状态发生变化时
             * @param device
             * @param status
             * @param newState
             */
            override fun onConnectionStateChange(
                device: BluetoothDevice,
                status: Int,
                newState: Int
            ) {
                addTip(

                    String.format(
                        "1.onConnectionStateChange：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )
                addTip(

                    String.format(
                        "1.onConnectionStateChange：status = %s, newState =%s ",
                        status,
                        newState
                    )
                )
                super.onConnectionStateChange(device, status, newState)
            }

            override fun onServiceAdded(status: Int, service: BluetoothGattService) {
                super.onServiceAdded(status, service)
                addTip(String.format("onServiceAdded：status = %s", status))
            }

            override fun onCharacteristicReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic
            ) {
                addTip(

                    String.format(
                        "onCharacteristicReadRequest：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )
                addTip(

                    String.format(
                        "onCharacteristicReadRequest：requestId = %s, offset = %s",
                        requestId,
                        offset
                    )
                )
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    characteristic.value
                )
                //            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            /**
             * 3. onCharacteristicWriteRequest,接收具体的字节
             * @param device
             * @param requestId
             * @param characteristic
             * @param preparedWrite
             * @param responseNeeded
             * @param offset
             * @param requestBytes
             */
            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                requestBytes: ByteArray
            ) {
                addTip(
                    String.format(
                        "3.onCharacteristicWriteRequest：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )
                addTip(String(requestBytes))
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    requestBytes
                )
                //4.处理响应内容
                onResponseToClient(requestBytes, device, requestId, characteristic)
            }

            /**
             * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
             * @param device
             * @param requestId
             * @param descriptor
             * @param preparedWrite
             * @param responseNeeded
             * @param offset
             * @param value
             */
            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                addTip(

                    String.format(
                        "2.onDescriptorWriteRequest：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )

                // now tell the connected device that this was all successfull
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }

            /**
             * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
             * @param device
             * @param requestId
             * @param offset
             * @param descriptor
             */
            override fun onDescriptorReadRequest(
                device: BluetoothDevice,
                requestId: Int,
                offset: Int,
                descriptor: BluetoothGattDescriptor
            ) {
                addTip(
                    String.format(
                        "onDescriptorReadRequest：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )
                addTip(String.format("onDescriptorReadRequest：requestId = %s", requestId))
                //            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                bluetoothGattServer.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    null
                )
            }

            override fun onNotificationSent(device: BluetoothDevice, status: Int) {
                super.onNotificationSent(device, status)
                addTip(

                    String.format(
                        "5.onNotificationSent：device name = %s, address = %s",
                        device.name,
                        device.address
                    )
                )
                addTip(String.format("5.onNotificationSent：status = %s", status))
            }

            override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
                super.onMtuChanged(device, mtu)
                addTip(String.format("onMtuChanged：mtu = %s", mtu))
            }

            override fun onExecuteWrite(device: BluetoothDevice, requestId: Int, execute: Boolean) {
                super.onExecuteWrite(device, requestId, execute)
                addTip(String.format("onExecuteWrite：requestId = %s", requestId))
            }
        }

    /**
     * 4.处理响应内容
     *
     * @param reqeustBytes
     * @param device
     * @param requestId
     * @param characteristic
     */
    private fun onResponseToClient(
        reqeustBytes: ByteArray,
        device: BluetoothDevice,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        addTip(
            String.format(
                "4.onResponseToClient：device name = %s, address = %s",
                device.name,
                device.address
            )
        )
        addTip(String.format("4.onResponseToClient：requestId = %s", requestId))
        val msg = String(reqeustBytes)
        println("4.收到:$msg")
        val str = String(reqeustBytes) + " hello>"
        characteristicRead.value = str.toByteArray()
        bluetoothGattServer.notifyCharacteristicChanged(device, characteristicRead, false)
        println("4.响应:$str")
    }
}