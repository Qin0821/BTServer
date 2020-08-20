package com.dragonfly.bt.btclient

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.clj.fastble.BleManager
import com.kq.btb.toStr
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private val TAG = "BTClient"
    val XXH_UUID = UUID.fromString("33719b35-639a-4edc-b9bc-345cf8bf3829")
//    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private lateinit var mAdapter: BaseQuickAdapter<BluetoothDevice, BaseViewHolder>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvContent.text = "配对UUID：$XXH_UUID \n" +
                "已配对设备:\n" +
                getBTAdapter().bondedDevices?.map {
                    it.name + " " + it.address + "\n"

                }
        getConnectBt()

        PermissionUtils.permission(PermissionConstants.LOCATION, PermissionConstants.STORAGE)
            .callback { isAllGranted, _, _, _ ->
                if (!isAllGranted) finish()
            }.request()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(1, 5000)
            .setSplitWriteNum(20)
            .setConnectOverTime(10000)
            .setOperateTimeout(5000);

        btConnect.setOnClickListener {

            Log.e(TAG, "start connect")
            mAdapter.setNewInstance(ArrayList())
            if (getBTAdapter().isDiscovering) getBTAdapter().cancelDiscovery()
            val result = getBTAdapter().startDiscovery()
            Log.e(TAG, result.toString())
        }

        mAdapter = object : BaseQuickAdapter<BluetoothDevice, BaseViewHolder>(R.layout.item_bt) {
            override fun convert(holder: BaseViewHolder, item: BluetoothDevice) {
                holder.setText(R.id.tvBt, item.toStr())
            }
        }
        mAdapter.addChildClickViewIds(R.id.tvBt)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->

            getBTAdapter().cancelDiscovery()

            val device = adapter.getItem(position) as BluetoothDevice
            ConnectThread(device).start()
        }
        rvBt.layoutManager = LinearLayoutManager(this)
        rvBt.adapter = mAdapter
    }

    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device == null) {
                        Log.e(TAG, "bt device is null")
                        return
                    }
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address

                    Log.e(TAG, "name: $deviceName mac: $deviceHardwareAddress")
                    mAdapter.addData(device)
                }
            }
        }
    }

    private inner class ConnectThread(val device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(XXH_UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            getBTAdapter().cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    ToastUtils.showShort("正在配对${device.address}")
                    socket.connect()

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket)
                } catch (e: Exception) {
                    ToastUtils.showShort("配对失败，请打开BTServer")
                    e.printStackTrace()
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    fun getBTAdapter(): BluetoothAdapter {
        return BluetoothAdapter.getDefaultAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }


    @SuppressLint("SetTextI18n")
    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        ToastUtils.showShort("配对成功")
        Log.e(TAG, socket.toString())
        getConnectBt()
    }

    //检查已连接的蓝牙设备
    private fun getConnectBt() {
        val a2dp: Int = getBTAdapter().getProfileConnectionState(BluetoothProfile.A2DP)
        val headset: Int = getBTAdapter().getProfileConnectionState(BluetoothProfile.HEADSET)
        val health: Int = getBTAdapter().getProfileConnectionState(BluetoothProfile.HEALTH)
        var flag = -1
        if (a2dp == BluetoothProfile.STATE_CONNECTED) {
            flag = a2dp
        } else if (headset == BluetoothProfile.STATE_CONNECTED) {
            flag = headset
        } else if (health == BluetoothProfile.STATE_CONNECTED) {
            flag = health
        }
        if (flag != -1) {
            getBTAdapter().getProfileProxy(this@MainActivity, object :
                BluetoothProfile.ServiceListener {
                override fun onServiceDisconnected(profile: Int) {
                    Toast.makeText(this@MainActivity, profile.toString() + "", Toast.LENGTH_SHORT)
                        .show()
                }

                @SuppressLint("SetTextI18n")
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    val mDevices: List<BluetoothDevice>? = proxy.connectedDevices
                    if (!mDevices.isNullOrEmpty()) {
                        tvContent.text = "配对UUID：$XXH_UUID \n" +
                                "已配对设备:\n" +
                                getBTAdapter().bondedDevices?.map {
                                    it.name + " " + it.address + "\n"
                                } + "已连接设备：\n" + mDevices.map {
                            it.name + "," + it.address + "\n"
                        }
                    }
                }
            }, flag)
        }
    }
}