package com.dragonfly.bt.server


import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ToastUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "BTServer"
        val XXH_UUID: UUID = UUID.fromString("33719b35-639a-4edc-b9bc-345cf8bf3829")
        val mBTAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        const val MESSAGE_READ: Int = 0
        const val MESSAGE_WRITE: Int = 1
        const val MESSAGE_TOAST: Int = 2
    }

    private var mConnectedThread: ConnectedThread? = null

    private lateinit var mAcceptThread: AcceptThread

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MESSAGE_READ -> {
                    try {
                        addTip("收到消息:")
                        addTip(String(msg.obj as ByteArray))
                    } catch (e: Exception) {
                        addTip("出错了:${e.message}")
                    }
                }
                MESSAGE_WRITE -> {
                    Log.e(TAG, "write " + msg.arg1 + msg.arg2 + msg.obj)
                }
                MESSAGE_TOAST -> {
                    Log.e(TAG, "toast " + msg.arg1 + msg.arg2 + msg.obj)
                    addTip(msg.obj.toString())
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvUUID.text = "配对UUID：$XXH_UUID \n" +
                "已配对设备:\n" +
                mBTAdapter.bondedDevices?.map {
                    it.name + " " + it.address + "\n"
                }

//        btDisConnect.setOnClickListener {
//            mAcceptThread.cancel()
//        }

        initListener()

        btReListener.setOnClickListener {
            initListener()
            tvRead.text = ""
            addTip("正在监听配对")
        }
    }

    fun addTip(msg: String) {
        runOnUiThread {
            tvRead.text = "${tvRead.text}\n$msg"
        }
    }

    private fun initListener() {
        mAcceptThread = AcceptThread {
            addTip("连接成功")
            Log.e(TAG, it.toString())

            mConnectedThread = ConnectedThread(mHandler, it)
            mConnectedThread!!.start()
        }
        mAcceptThread.start()
    }
}