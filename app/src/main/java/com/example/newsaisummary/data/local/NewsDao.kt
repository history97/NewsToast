package com.example.newsaisummary.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.newsaisummary.data.model.NewsArticle
import kotlinx.coroutines.flow.Flow

/**
 * News Data Access Object
 * 뉴스 기사 데이터베이스 접근을 위한 인터페이스
 */
@Dao
interface NewsDao {

    /**
     * 모든 뉴스 기사 조회 (최신순 정렬)
     * Flow를 사용하여 실시간 업데이트 감지
     */
    @Query("SELECT * FROM news_articles ORDER BY timestamp DESC")
    fun getAllNews(): Flow<List<NewsArticle>>

    /**
     * 카테고리별 뉴스 조회
     */
    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY timestamp DESC")
    fun getNewsByCategory(category: String): Flow<List<NewsArticle>>

    /**
     * ID로 특정 뉴스 기사 조회
     */
    @Query("SELECT * FROM news_articles WHERE id = :id")
    suspend fun getNewsById(id: Long): NewsArticle?

    /**
     * 뉴스 기사 삽입
     * 중복 시 기존 데이터 대체
     * @return 삽입된 행의 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(article: NewsArticle): Long

    /**
     * 뉴스 기사 업데이트
     */
    @Update
    suspend fun updateNews(article: NewsArticle)

    /**
     * 뉴스 기사 삭제
     */
    @Delete
    suspend fun deleteNews(article: NewsArticle)

    /**
     * 모든 뉴스 기사 삭제
     */
    @Query("DELETE FROM news_articles")
    suspend fun deleteAllNews()

    /**
     * 요약된 뉴스만 조회
     */
    @Query("SELECT * FROM news_articles WHERE isSummarized = 1 ORDER BY timestamp DESC")
    fun getSummarizedNews(): Flow<List<NewsArticle>>

    /**
     * 특정 기간의 뉴스 조회
     */
    @Query("SELECT * FROM news_articles WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getNewsByDateRange(startTime: Long, endTime: Long): Flow<List<NewsArticle>>
    
    /**
     * 즐겨찾기 토글
     */
    @Query("UPDATE news_articles SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
    
    /**
     * 즐겨찾기한 뉴스만 조회
     */
    @Query("SELECT * FROM news_articles WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNews(): Flow<List<NewsArticle>>
}
