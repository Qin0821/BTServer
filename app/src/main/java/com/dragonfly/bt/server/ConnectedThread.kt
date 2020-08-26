package com.dragonfly.bt.server

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.dragonfly.bt.server.MainActivity.Companion.MESSAGE_READ
import com.dragonfly.bt.server.MainActivity.Companion.MESSAGE_TOAST
import com.dragonfly.bt.server.MainActivity.Companion.MESSAGE_WRITE
import com.dragonfly.bt.server.MainActivity.Companion.TAG
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ConnectedThread(private val mHandler: Handler,  private val mmSocket: BluetoothSocket) : Thread() {

    private val mmInStream: InputStream = mmSocket.inputStream
    private val mmOutStream: OutputStream = mmSocket.outputStream
    private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

    override fun run() {
        var numBytes: Int // bytes returned from read()

        mHandler.obtainMessage(MESSAGE_TOAST, -1, -1, "开始监听")
        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            // Read from the InputStream.
            numBytes = try {
                mmInStream.read(mmBuffer)
            } catch (e: Exception) {
                mHandler.obtainMessage(MESSAGE_TOAST, -1, -1, "停止监听: ${e.message}")
                Log.d(TAG, "Input stream was disconnected", e)
                break
            }

            // Send the obtained bytes to the UI activity.
            val readMsg = mHandler.obtainMessage(
                MESSAGE_READ, numBytes, -1,
                mmBuffer
            )
            readMsg.sendToTarget()
        }
    }

    // Call this from the main activity to send data to the remote device.
    fun write(bytes: ByteArray) {
        try {
            mmOutStream.write(bytes)
        } catch (e: IOException) {
            Log.e(TAG, "Error occurred when sending data", e)

            // Send a failure message back to the activity.
            val writeErrorMsg = mHandler.obtainMessage(MESSAGE_TOAST)
            val bundle = Bundle().apply {
                putString("toast", "Couldn't send data to the other device")
            }
            writeErrorMsg.data = bundle
            mHandler.sendMessage(writeErrorMsg)
            return
        }

        // Share the sent message with the UI activity.
        val writtenMsg = mHandler.obtainMessage(
            MESSAGE_WRITE, -1, -1, mmBuffer
        )
        writtenMsg.sendToTarget()
    }

    // Call this method from the main activity to shut down the connection.
    fun cancel() {
        try {
            mmSocket.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }
}