package com.example.forgeint

import com.google.gson.annotations.SerializedName

data class RemoteImageOptions(
    @SerializedName("maxDimensionPx") val maxDimensionPx: Int = 360,
    @SerializedName("jpegQuality") val jpegQuality: Int = 58
) {
    fun normalized(): RemoteImageOptions = copy(
        maxDimensionPx = maxDimensionPx.coerceIn(160, 1024),
        jpegQuality = jpegQuality.coerceIn(35, 90)
    )

    companion object {
        fun watchPreview(
            maxDimensionPx: Int = 360,
            jpegQuality: Int = 58
        ): RemoteImageOptions = RemoteImageOptions(
            maxDimensionPx = maxDimensionPx,
            jpegQuality = jpegQuality
        ).normalized()
    }
}

data class RemoteRequest(
    @SerializedName("url") val url: String,
    @SerializedName("method") val method: String = "GET",
    @SerializedName("headers") val headers: Map<String, String> = emptyMap(),
    @SerializedName("body") val body: String? = null,
    @SerializedName("imageOptions") val imageOptions: RemoteImageOptions? = null,
    @SerializedName("requestId") val requestId: String = java.util.UUID.randomUUID().toString()
)
