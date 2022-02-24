package com.example.wschatapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var sendBtn: Button
    private lateinit var connectBtn: Button
    private lateinit var etMessage: EditText
    private lateinit var etName: EditText
    private lateinit var messagesLayout: LinearLayout
    var message = ""
    private lateinit var myLayout: View

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendBtn = findViewById(R.id.btn_send)
        connectBtn = findViewById(R.id.btn_connect)
        etMessage = findViewById(R.id.et_message)
        etName = findViewById(R.id.et_name)
        messagesLayout = findViewById(R.id.messages_layout)

        connectBtn.setOnClickListener {
            if(etName.text.isBlank()) {
                addText("Enter your name before connecting")
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    runClient(etName.text.toString())
                }
            }
        }

        sendBtn.setOnClickListener {
            message = etMessage.text.toString()
            etMessage.text.clear()
        }
    }

    private suspend fun runClient(name: String) {
        client.webSocket(HttpMethod.Get, "192.168.182.174", 8080, "/wschat") {
            val messageOutputRoutine = launch { outputMessages() }  // Launch output coroutine
            val userInputRoutine = launch { inputMessages() }  // Launch input coroutine

            // Make this coroutine wait for these two to finish (I think!)
            userInputRoutine.join()
            messageOutputRoutine.cancelAndJoin()
        }
        client.close()
        Log.d(TAG, "Connection closed")
    }

    private suspend fun DefaultClientWebSocketSession.outputMessages() {
        try {
            for(message in incoming) {
                message as? Frame.Text ?: continue
                val messageText = message.readText()
                Log.d(TAG, "Received message: $messageText")
                runOnUiThread{
                    addText(messageText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "outputMessages: Error receiving message\n${e.message}")
        }
    }

    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        while(true){
            if (message == "") continue
            try {
                Log.d(TAG, "Sending message: $message")
                send(message)
                message = ""
            } catch (e: Exception){
                Log.e(TAG, "inputMessages: failed to send message\n${e.message}")
                return
            }
        }
    }

    private fun addText(text: String){
        myLayout = layoutInflater.inflate(
            R.layout.received_message,
            messagesLayout,
            false)
        myLayout.findViewById<TextView>(R.id.message).text = text
        messagesLayout.addView(myLayout)
    }
}