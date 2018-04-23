package com.eyeclick.sergb.producer

import android.app.Service
import android.content.Intent
import android.os.*
import android.widget.Toast
import java.io.EOFException


class MessengerService : Service() {

    val REQUEST_BOOK = 101
    val READ_BULK = 201
    val READ_END = 202

    private var mMessenger = Messenger(IncomingHandler())

    // message handle from client app
    internal inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                REQUEST_BOOK -> {

                    ReadBookInChunks(msg)

                    // show toast to look cool
                    Toast.makeText(applicationContext, "Done Reading", Toast.LENGTH_SHORT).show()
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    // read book.txt file and send content to client in bulk of 1000 chars
    private fun ReadBookInChunks(msg: Message) {
        val book = application.assets.open("book.txt").reader().buffered(DEFAULT_BUFFER_SIZE)

        var t: String = ""
        while (book.ready()) {
            try {
                t += book.read().toChar()
            } catch (e: EOFException) {
                //end of file
            }

            if (t.length >= 1000) {
                sendChunk(t, msg)
                t = ""
            }
        }

        //send last chunk of data
        if (t.isNotEmpty()) {
            sendChunk(t, msg)
        }

        //send done action to client app
        val msg2 = Message.obtain(null, READ_END, 0, 0)
        try {
            msg.replyTo.send(msg2)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    // send chunk of text to client app
    private fun sendChunk(chunk: String, msg: Message) {
        val msg2 = Message.obtain(null, READ_BULK, 0, 0)
        val bundle = Bundle()
        bundle.putString("chunk", chunk)
        msg2.data = bundle
        try {

            // the replayTo passed from client app
            msg.replyTo.send(msg2)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onCreate() {
    }

    // bind service and return messenger
    override fun onBind(intent: Intent): IBinder? {
        return mMessenger.binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        return false
    }
}