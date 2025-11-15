package com.example.superai.network

import android.util.Log
import com.example.superai.model.BackendRequest
import com.example.superai.model.BackendResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        install(DefaultRequest) {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
        }
    }

    private const val BACKEND_URL = "https://super-ai-backend.onrender.com/api/generate"

    suspend fun send(request: BackendRequest): BackendResponse {
        return try {
            val response: HttpResponse = client.post(BACKEND_URL) {
                setBody(request)
            }
            if (response.status.isSuccess()) {
                response.bodyAsText().let {
                    Json.decodeFromString(it)
                }
            } else {
                Log.e("ApiClient", "Error: ${response.status.value} ${response.status.description}")
                BackendResponse(null, error = "Server error: ${response.status.value}")
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception: ${e.message}", e)
            BackendResponse(null, error = "Network error: ${e.message}")
        }
    }
}
