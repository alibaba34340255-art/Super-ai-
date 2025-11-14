package com.example.superai.data

import kotlinx.serialization.Serializable

@Serializable
data class BackendRequest(val prompt: String, val mode: String, val custom_api_key: String? = null)
