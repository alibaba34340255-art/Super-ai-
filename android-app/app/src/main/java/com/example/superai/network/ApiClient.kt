package com.example.superai.network

import com.example.superai.data.BackendRequest
import com.example.superai.data.BackendResponse
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        engine {
            config {
                connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
    }

    private const val BACKEND_URL = "http://10.0.2.2:5000/api/generate"

    suspend fun send(request: BackendRequest): BackendResponse {
        return client.post(BACKEND_URL) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.bodyAsText().let {
            Json.decodeFromString(it)
        }
    }
}
