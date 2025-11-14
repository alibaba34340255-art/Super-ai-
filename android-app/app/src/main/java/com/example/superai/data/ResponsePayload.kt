package com.example.superai.data

import kotlinx.serialization.Serializable

@Serializable
data class ResponsePayload(
    val text: String,
    val image_url: String? = null
)
