package com.example.newsaisummary.data.repository

import com.example.newsaisummary.data.api.ChatCompletionRequest
import com.example.newsaisummary.data.api.Message
import com.example.newsaisummary.data.api.OpenAiApi
import com.example.newsaisummary.data.config.ApiKeyManager
import com.example.newsaisummary.data.local.NewsDao
import com.example.newsaisummary.data.model.NewsArticle
import com.example.newsaisummary.data.model.NewsCategory
import com.example.newsaisummary.data.model.NewsItem
import com.example.newsaisummary.data.scraper.NewsScraper
import kotlinx.coroutines.flow.Flow

class NewsRepository(
    private val newsDao: NewsDao,
    private val openAiApi: OpenAiApi,
    private val newsScraper: NewsScraper
) {

    val allNews: Flow<List<NewsArticle>> = newsDao.getAllNews()

    fun getNewsByCategory(category: NewsCategory): Flow<List<NewsArticle>> {
        return if (category == NewsCategory.ALL) {
            allNews
        } else {
            newsDao.getNewsByCategory(category.name)
        }
    }

    suspend fun fetchLatestNews(category: NewsCategory = NewsCategory.ALL): List<NewsItem> {
        return newsScraper.scrapeNaverNews(category)
    }

    /**
     * 키워드로 뉴스 검색 (네이버 API 사용)
     * 검색어가 포함된 기사를 정확도순으로 가져온다
     */
    suspend fun searchNews(
        keyword: String,
        maxResults: Int = 30
    ): List<NewsItem> {
        return try {
            // 네이버 API 검색 시도 (정확도순)
            val apiResults = newsScraper.searchNews(keyword, maxResults, sortByDate = false)
            
            if (apiResults.isNotEmpty()) {
                println("[NewsRepository] 네이버 API 검색 성공 (정확도순): ${apiResults.size}개")
                return apiResults
            }
            
            // API 실패시 크롤링으로 대체 (정확도순)
            println("[NewsRepository] API 실패, 크롤링 시도 (정확도순)")
            newsScraper.searchNaverNews(keyword, maxResults, sortByDate = false)
        } catch (e: Exception) {
            println("[NewsRepository] 검색 에러: ${e.message}")
            emptyList()
        }
    }

    suspend fun getArticleContent(url: String): String {
        return newsScraper.scrapeArticleContent(url)
    }

    suspend fun summarizeWithAi(content: String): Result<String> {
        return try {
            val apiKey = ApiKeyManager.getOpenAiApiKey()

            if (!ApiKeyManager.isApiKeySet()) {
                return Result.failure(
                    Exception("API 키가 설정되지 않았습니다.\nApiKeyManager.kt 파일에서 API_KEY를 설정해주세요.")
                )
            }

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    Message(
                        role = "system",
                        content = """
                            당신은 뉴스 기사를 간결하게 요약하는 AI 어시스턴트입니다.
                            다음 규칙을 따라주세요:
                            1. 핵심 내용을 3-5문장으로 요약
                            2. 중요한 사실과 수치 포함
                            3. 객관적이고 중립적인 톤 유지
                            4. 한국어로 답변
                        """.trimIndent()
                    ),
                    Message(
                        role = "user",
                        content = "다음 뉴스 기사를 요약해주세요:\n\n$content"
                    )
                ),
                max_tokens = 500,
                temperature = 0.7
            )

            val response = openAiApi.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            val summary = response.choices.firstOrNull()?.message?.content
                ?: "요약을 생성할 수 없습니다."

            Result.success(summary.trim())
        } catch (e: Exception) {
            e.printStackTrace()

            val errorMessage = when {
                e.message?.contains("401") == true ->
                    "API 키가 유효하지 않습니다. OpenAI API 키를 확인해주세요."
                e.message?.contains("429") == true ->
                    "API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."
                e.message?.contains("timeout") == true ->
                    "요청 시간이 초과되었습니다. 네트워크 연결을 확인해주세요."
                else ->
                    "요약 생성 중 오류가 발생했습니다: ${e.message}"
            }

            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun saveArticle(article: NewsArticle): Long {
        return newsDao.insertNews(article)
    }

    suspend fun updateArticle(article: NewsArticle) {
        newsDao.updateNews(article)
    }

    suspend fun deleteArticle(article: NewsArticle) {
        newsDao.deleteNews(article)
    }

    suspend fun getArticleById(id: Long): NewsArticle? {
        return newsDao.getNewsById(id)
    }

    suspend fun deleteAllNews() {
        newsDao.deleteAllNews()
    }
    
    suspend fun toggleFavorite(article: NewsArticle) {
        newsDao.toggleFavorite(article.id, !article.isFavorite)
    }
}
