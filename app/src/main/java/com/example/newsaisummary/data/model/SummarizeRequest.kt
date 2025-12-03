package com.example.newsaisummary.data.model

/**
 * 뉴스 아이템과 테스트 모드 정보를 담는 래퍼 클래스
 */
data class SummarizeRequest(
    val newsItem: NewsItem,
    val isTest: Boolean = false
)
