package com.example.forgeint.presentation

import android.content.Context
import com.example.forgeint.RemoteRequest
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.charset.StandardCharsets

class PhoneCommunicator(private val context: Context) : MessageClient.OnMessageReceivedListener {

    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val gson = Gson()
    
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<String>>()
    private val pendingPings = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    data class RemoteResponse(
        val requestId: String,
        val response: String
    )

    init {
        messageClient.addListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == PATH_API_RESPONSE) {
            val responseData = String(messageEvent.data, StandardCharsets.UTF_8)
            try {
                val remoteResponse = gson.fromJson(responseData, RemoteResponse::class.java)
                val deferred = pendingRequests.remove(remoteResponse.requestId)
                deferred?.complete(remoteResponse.response)
            } catch (e: Exception) {
                // If parsing fails, it might be an old format message or error
                // In a transitional state, we can't easily recover without ID.
            }
        } else if (messageEvent.path == PATH_PONG) {
            val requestId = String(messageEvent.data, StandardCharsets.UTF_8).trim()
            val deferred = pendingPings.remove(requestId)
            deferred?.complete(Unit)
        }
    }

    suspend fun sendRequest(request: RemoteRequest): String? {
        // 1. Find the connected phone
        val targetNodeId = resolveTargetNodeId()

        if (targetNodeId == null) {
            return "Error: No phone connected"
        }

        // 2. Prepare the request
        val requestId = request.requestId
        val jsonRequest = gson.toJson(request)
        
        val deferred = CompletableDeferred<String>()
        pendingRequests[requestId] = deferred

        // 3. Send the message
        try {
            messageClient.sendMessage(
                targetNodeId,
                PATH_API_REQUEST,
                jsonRequest.toByteArray()
            ).await()
        } catch (e: Exception) {
            pendingRequests.remove(requestId)
            return "Error sending message: ${e.message}"
        }

        // 4. Wait for response (with timeout)
        return try {
            withTimeoutOrNull(45000) { // Increased timeout to 45s
                deferred.await()
            } ?: run {
                pendingRequests.remove(requestId)
                "Error: Timeout waiting for phone response"
            }
        } catch (e: Exception) {
            pendingRequests.remove(requestId)
            "Error: ${e.message}"
        }
    }

    suspend fun pingPhone(timeoutMs: Long = 3000L): Boolean {
        val targetNodeId = resolveTargetNodeId() ?: return false

        val requestId = java.util.UUID.randomUUID().toString()
        val deferred = CompletableDeferred<Unit>()
        pendingPings[requestId] = deferred

        return try {
            messageClient.sendMessage(
                targetNodeId,
                PATH_PING,
                requestId.toByteArray(StandardCharsets.UTF_8)
            ).await()

            withTimeoutOrNull(timeoutMs) { deferred.await() } != null
        } catch (_: Exception) {
            false
        } finally {
            pendingPings.remove(requestId)
        }
    }

    private suspend fun resolveTargetNodeId(): String? {
        val capabilityNodes = try {
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes
        } catch (_: Exception) {
            emptySet()
        }

        val preferred = capabilityNodes.firstOrNull { it.isNearby } ?: capabilityNodes.firstOrNull()
        if (preferred != null) return preferred.id

        val connectedNodes = try {
            nodeClient.connectedNodes.await()
        } catch (_: Exception) {
            emptyList()
        }
        return connectedNodes.firstOrNull { it.isNearby }?.id ?: connectedNodes.firstOrNull()?.id
    }
    
    fun cleanup() {
        messageClient.removeListener(this)
    }

    companion object {
        private const val CAPABILITY_PHONE = "forgeint_phone"
        private const val PATH_API_REQUEST = "/api-request"
        private const val PATH_API_RESPONSE = "/api-response"
        private const val PATH_PING = "/ping"
        private const val PATH_PONG = "/pong"
    }
}
