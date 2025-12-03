package com.example.newsaisummary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 데이터베이스에 저장되는 뉴스 기사 엔티티
 */
@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val content: String,
    val summary: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSummarized: Boolean = false,
    val category: String = "ALL",
    val isFavorite: Boolean = false,
    val isTest: Boolean = false  // 테스트용 기사 표시
)

/**
 * 크롤링된 뉴스 아이템 (저장 전)
 */
data class NewsItem(
    val title: String,
    val url: String,
    val content: String = "",
    val category: NewsCategory = NewsCategory.ALL,
    val publishDate: String = ""  // 발행 날짜 추가
)
