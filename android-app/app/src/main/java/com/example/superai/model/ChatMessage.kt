package com.example.superai.model

import kotlinx.serialization.Serializable

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageUrl: String? = null,
    val modelUsed: String? = null,
    val diagnosticReport: String? = null
)
