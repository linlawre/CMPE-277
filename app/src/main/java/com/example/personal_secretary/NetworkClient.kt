package com.example.personal_secretary

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object NetworkClient {

    // ----------------- HttpClient -----------------
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }
            )
        }
    }

    // ----------------- Data classes -----------------
    @Serializable
    data class AuthRequest(val email: String, val password: String)

    @Serializable
    data class AuthResponse(
        val success: Boolean,
        val token: String? = null,
        val message: String? = null
    )

    // ----------------- Functions -----------------
    suspend fun login(email: String, password: String): AuthResponse {
        return client.post("http://10.0.2.2:4000/login") { // <-- changed here
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }.body()
    }

    suspend fun signup(email: String, password: String): AuthResponse {
        return client.post("http://10.0.2.2:4000/signup") { // <-- changed here
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }.body()
    }
}
