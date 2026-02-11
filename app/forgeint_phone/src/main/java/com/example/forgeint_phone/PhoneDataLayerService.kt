package com.example.forgeint

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PhoneDataLayerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient()

    data class RemoteResponse(
        val requestId: String,
        val response: String
    )

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_PING) {
            val requestId = String(messageEvent.data, Charsets.UTF_8).trim()
            sendPong(messageEvent.sourceNodeId, requestId)
            return
        }

        if (messageEvent.path == PATH_API_REQUEST) {
            val requestData = String(messageEvent.data, Charsets.UTF_8)
            Log.d(TAG, "Received request: $requestData")

            serviceScope.launch {
                val gson = com.google.gson.Gson()
                try {
                    val remoteRequest = gson.fromJson(requestData, RemoteRequest::class.java)
                    val responseData = performNetworkRequest(remoteRequest)
                    
                    val responseWrapper = RemoteResponse(
                        requestId = remoteRequest.requestId,
                        response = responseData
                    )
                    
                    sendResponse(messageEvent.sourceNodeId, gson.toJson(responseWrapper))
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing request", e)
                    // Parsing failure means we don't have a requestId to reply with.
                }
            }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }

    private fun performNetworkRequest(remoteRequest: RemoteRequest): String {
        return try {
            val requestBuilder = Request.Builder()
                .url(remoteRequest.url)

            remoteRequest.headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            if (remoteRequest.method.equals("POST", ignoreCase = true) && remoteRequest.body != null) {
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = okhttp3.RequestBody.create(mediaType, remoteRequest.body)
                requestBuilder.post(body)
            } else if (remoteRequest.method.equals("GET", ignoreCase = true)) {
                requestBuilder.get()
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                 if (!response.isSuccessful) {
                     "Error: ${response.code} ${response.message}"
                 } else {
                     response.body?.string() ?: ""
                 }
            }
        } catch (e: Exception) {
            "Network Error: ${e.message}"
        }
    }

    private fun sendResponse(nodeId: String, response: String) {
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PATH_API_RESPONSE, response.toByteArray())
            .addOnSuccessListener {
                Log.d(TAG, "Response sent to $nodeId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to send response", it)
            }
    }

    private fun sendPong(nodeId: String, requestId: String) {
        Wearable.getMessageClient(this)
            .sendMessage(nodeId, PATH_PONG, requestId.toByteArray(Charsets.UTF_8))
            .addOnSuccessListener {
                Log.d(TAG, "Pong sent to $nodeId")
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to send pong", it)
            }
    }

    override fun onCreate() {
        super.onCreate()
        Wearable.getCapabilityClient(this)
            .addLocalCapability(CAPABILITY_PHONE)
            .addOnSuccessListener { Log.d(TAG, "Capability registered") }
            .addOnFailureListener { Log.e(TAG, "Capability registration failed", it) }
    }

    override fun onDestroy() {
        Wearable.getCapabilityClient(this)
            .removeLocalCapability(CAPABILITY_PHONE)
            .addOnFailureListener { Log.e(TAG, "Capability removal failed", it) }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PhoneDataLayerService"
        private const val CAPABILITY_PHONE = "forgeint_phone"
        private const val PATH_API_REQUEST = "/api-request"
        private const val PATH_API_RESPONSE = "/api-response"
        private const val PATH_PING = "/ping"
        private const val PATH_PONG = "/pong"
    }
}

