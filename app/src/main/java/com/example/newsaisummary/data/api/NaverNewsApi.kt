package com.example.newsaisummary.data.api

import com.example.newsaisummary.data.model.NaverNewsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 네이버 검색 API 인터페이스
 * 문서: https://developers.naver.com/docs/serviceapi/search/news/news.md
 */
interface NaverNewsApi {
    /**
     * 뉴스 검색
     * @param query 검색어 (필수)
     * @param display 한 번에 표시할 검색 결과 개수(기본값: 10, 최댓값: 100)
     * @param start 검색 시작 위치(기본값: 1, 최댓값: 1000)
     * @param sort 검색 결과 정렬 방법 (sim: 정확도순, date: 날짜순)
     * @param clientId 네이버 애플리케이션 클라이언트 아이디
     * @param clientSecret 네이버 애플리케이션 클라이언트 시크릿
     */
    @GET("v1/search/news.json")
    suspend fun searchNews(
        @Query("query") query: String,
        @Query("display") display: Int = 30,
        @Query("start") start: Int = 1,
        @Query("sort") sort: String = "date",
        @Header("X-Naver-Client-Id") clientId: String,
        @Header("X-Naver-Client-Secret") clientSecret: String
    ): NaverNewsResponse
}
