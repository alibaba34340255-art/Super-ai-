package com.example.superai.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendRequest(
    val prompt: String,
    val mode: String,
    val userId: String? = null,
    val options: Map<String, String>? = null
)
