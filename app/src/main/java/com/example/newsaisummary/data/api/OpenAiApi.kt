package com.example.newsaisummary.data.api

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * OpenAI Chat Completion API 인터페이스
 */
interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}

/**
 * Chat Completion 요청 데이터
 */
data class ChatCompletionRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val max_tokens: Int = 500,
    val temperature: Double = 0.7
)

/**
 * 대화 메시지
 */
data class Message(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * Chat Completion 응답 데이터
 */
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage
)

/**
 * 응답 선택지
 */
data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

/**
 * 토큰 사용량 정보
 */
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)