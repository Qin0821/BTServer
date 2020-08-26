package com.dragonfly.btserver


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
        val XXH_UUID = UUID.fromString("33719b35-639a-4edc-b9bc-345cf8bf3829")
        val mBTAdapter = BluetoothAdapter.getDefaultAdapter()

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
                    var result = "收到消息:"
                    result = result + msg.toString()
                    Log.e(TAG, "read " + msg.arg1 + msg.arg2 + msg.obj)
                    Log.e(TAG, String(msg.obj as ByteArray))
                    runOnUiThread {
                        try {
                            result = result + " 准备显示消息"
                            tvRead.text = String(msg.obj as ByteArray)
                        } catch (e: Exception) {
                            tvRead.text = "出错了"
                            ToastUtils.showShort("read error: ${e.message}")
                        }
                        tvTip1.text = result
                    }
                }
                MESSAGE_WRITE -> {
                    Log.e(TAG, "write " + msg.arg1 + msg.arg2 + msg.obj)
                }
                MESSAGE_TOAST -> {
                    Log.e(TAG, "toast " + msg.arg1 + msg.arg2 + msg.obj)
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

        btDisConnect.setOnClickListener {
            mAcceptThread.cancel()
        }

        initListener()

        btReListener.setOnClickListener {
            initListener()
            tvTip1.text = ""
            ToastUtils.showShort("重新监听配对")
        }
    }

    private fun initListener() {
        mAcceptThread = AcceptThread {
            ToastUtils.showShort("连接成功")
            Log.e(TAG, it.toString())

            mConnectedThread = ConnectedThread(mHandler, it)
            mConnectedThread!!.start()
        }
        mAcceptThread.start()
    }
}