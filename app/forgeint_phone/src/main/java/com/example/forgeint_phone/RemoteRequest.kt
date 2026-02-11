package com.example.forgeint

import com.google.gson.annotations.SerializedName

data class RemoteRequest(
    @SerializedName("url") val url: String,
    @SerializedName("method") val method: String = "GET",
    @SerializedName("headers") val headers: Map<String, String> = emptyMap(),
    @SerializedName("body") val body: String? = null,
    @SerializedName("requestId") val requestId: String = java.util.UUID.randomUUID().toString()
)

