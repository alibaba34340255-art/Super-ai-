package com.example.superai.data

import kotlinx.serialization.Serializable

@Serializable
data class BackendResponse(
    val status: String,
    val response: ResponsePayload,
    val model_used: String,
    val diagnostic_report: String
)
