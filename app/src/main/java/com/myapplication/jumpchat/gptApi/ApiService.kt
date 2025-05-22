package com.myapplication.jumpchat.gptApi

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
interface ApiService {
    @POST("v1/chat/completions")
    suspend fun sendChat(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
