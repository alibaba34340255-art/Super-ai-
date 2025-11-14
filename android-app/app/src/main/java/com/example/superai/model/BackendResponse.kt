package com.example.superai.model

import kotlinx.serialization.Serializable

@Serializable
data class BackendResponse(
    val text: String?,
    val images: List<String>? = null,
    val diagnostics: Map<String, String>? = null,
    val tokensUsed: Int? = null,
    val error: String? = null
)
