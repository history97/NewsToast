package com.example.newsaisummary.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 싱글톤 인스턴스
 * OpenAI API 통신을 위한 Retrofit 설정
 */
object RetrofitInstance {
    private const val BASE_URL = "https://api.openai.com/"

    /**
     * HTTP 로깅 인터셉터
     * 개발 중 API 요청/응답을 로그로 확인
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttp 클라이언트 설정
     * - 로깅 인터셉터 추가
     * - 타임아웃 설정
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Retrofit 인스턴스 생성
     */
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * OpenAI API 인터페이스 구현체
     */
    val openAiApi: OpenAiApi = retrofit.create(OpenAiApi::class.java)
    
    // ========== 네이버 API 설정 ==========
    
    private const val NAVER_BASE_URL = "https://openapi.naver.com/"
    
    /**
     * 네이버 API용 Retrofit 인스턴스
     */
    private val naverRetrofit = Retrofit.Builder()
        .baseUrl(NAVER_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    /**
     * 네이버 뉴스 검색 API 인터페이스 구현체
     */
    val naverNewsApi: NaverNewsApi = naverRetrofit.create(NaverNewsApi::class.java)
}