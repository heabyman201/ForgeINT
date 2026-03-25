package com.example.forgeint.presentation

data class ChatAttachment(
    val label: String,
    val mimeType: String,
    val textContent: String? = null,
    val imageDataUrl: String? = null
)

data class ContentTextPart(
    val type: String = "text",
    val text: String
)

data class ImageUrlPayload(
    val url: String
)

data class ContentImagePart(
    val type: String = "image_url",
    val image_url: ImageUrlPayload
)
