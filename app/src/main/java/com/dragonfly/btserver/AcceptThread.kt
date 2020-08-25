package com.dragonfly.btserver

import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.dragonfly.btserver.MainActivity.Companion.TAG
import com.dragonfly.btserver.MainActivity.Companion.XXH_UUID
import com.dragonfly.btserver.MainActivity.Companion.mBTAdapter
import java.io.IOException

class AcceptThread(private val connectedCallback: (BluetoothSocket) -> Unit) : Thread() {
    private val NAME = "XXH"

    private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
        mBTAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, XXH_UUID)
    }

    override fun run() {
        // Keep listening until exception occurs or a socket is returned.
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                Log.e(TAG, "Socket's accept() method failed", e)
                shouldLoop = false
                null
            }
            socket?.also {
                connectedCallback.invoke(it)
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    // Closes the connect socket and causes the thread to finish.
    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }
}