package com.example.wschatapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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

    private lateinit var lilBtn: Button
    private lateinit var lilET: EditText
    var message = ""

    private val client = HttpClient(CIO) {
        install(WebSockets)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lilBtn = findViewById(R.id.button)
        lilET = findViewById(R.id.editText)

        CoroutineScope(Dispatchers.IO).launch {
            runClient()
        }

        lilBtn.setOnClickListener {
            message = lilET.text.toString()
            lilET.text.clear()
        }
    }

    private suspend fun runClient() {
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
                Log.d(TAG, "Server: ${message.readText()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "outputMessages: Error receiving message\n${e.message}")
        }
    }

    private suspend fun DefaultClientWebSocketSession.inputMessages() {
        while(true){
            if (message == "") continue
            if (message.equals("exit", true)) return
            try {
                send(message)
                message = ""
            } catch (e: Exception){
                Log.e(TAG, "inputMessages: failed to send message\n${e.message}")
                return
            }
        }
    }
}