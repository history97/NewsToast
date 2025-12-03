package com.example.newsaisummary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsaisummary.data.model.NewsArticle
import com.example.newsaisummary.data.model.NewsCategory
import com.example.newsaisummary.data.model.NewsItem
import com.example.newsaisummary.data.repository.NewsRepository
import com.example.newsaisummary.utils.NewsRecommendationUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NewsUiState(
    val latestNews: List<NewsItem> = emptyList(),
    val isLoadingNews: Boolean = false,
    val isLoadingContent: Boolean = false,
    val isSummarizing: Boolean = false,
    val selectedArticle: NewsArticle? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedCategory: NewsCategory = NewsCategory.ALL,
    val lastFetchedCategory: NewsCategory? = null
)

class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    val savedNews: StateFlow<List<NewsArticle>> = repository.allNews
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun selectCategory(category: NewsCategory) {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(selectedCategory = category)
        
        if (currentState.lastFetchedCategory != category) {
            fetchLatestNews(category)
        }
    }

    fun fetchLatestNews(category: NewsCategory = NewsCategory.ALL) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingNews = true,
                errorMessage = null
            )

            try {
                val news = repository.fetchLatestNews(category)
                _uiState.value = _uiState.value.copy(
                    latestNews = news,
                    isLoadingNews = false,
                    lastFetchedCategory = category,
                    successMessage = "${news.size}개의 ${category.displayName} 뉴스를 가져왔습니다."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingNews = false,
                    errorMessage = "뉴스를 가져오는 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    /**
     * 키워드로 뉴스 검색
     */
    fun searchNews(keyword: String) {
        if (keyword.isBlank()) {
            println("[NewsViewModel] 검색어가 비어있음")
            return
        }
        
        println("[NewsViewModel] searchNews 시작: keyword='$keyword'")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingNews = true,
                errorMessage = null,
                latestNews = emptyList()  // 기존 결과 초기화
            )

            try {
                println("[NewsViewModel] Repository.searchNews 호출")
                val news = repository.searchNews(keyword)
                
                println("[NewsViewModel] 검색 성공: ${news.size}개")
                
                _uiState.value = _uiState.value.copy(
                    latestNews = news,
                    isLoadingNews = false,
                    successMessage = "'$keyword' 검색 결과: ${news.size}개"
                )
            } catch (e: Exception) {
                println("[NewsViewModel] 검색 실패: ${e.message}")
                e.printStackTrace()
                
                _uiState.value = _uiState.value.copy(
                    isLoadingNews = false,
                    latestNews = emptyList(),
                    errorMessage = "검색 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun summarizeAndSaveArticle(newsItem: NewsItem, isTest: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingContent = true,
                isSummarizing = false,
                errorMessage = null
            )

            try {
                val content = repository.getArticleContent(newsItem.url)

                if (content.length < 100) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingContent = false,
                        errorMessage = "본문이 너무 짧거나 가져올 수 없습니다."
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    isLoadingContent = false,
                    isSummarizing = true
                )

                val summaryResult = repository.summarizeWithAi(content)

                summaryResult.fold(
                    onSuccess = { summary ->
                        val article = NewsArticle(
                            title = newsItem.title,
                            url = newsItem.url,
                            content = content,
                            summary = summary,
                            isSummarized = true,
                            timestamp = System.currentTimeMillis(),
                            category = newsItem.category.name,
                            isTest = isTest  // 테스트 모드 설정
                        )

                        repository.saveArticle(article)

                        _uiState.value = _uiState.value.copy(
                            isSummarizing = false,
                            selectedArticle = article,
                            successMessage = "뉴스가 성공적으로 요약되고 저장되었습니다."
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSummarizing = false,
                            errorMessage = error.message ?: "알 수 없는 오류가 발생했습니다."
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingContent = false,
                    isSummarizing = false,
                    errorMessage = "처리 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun deleteArticle(article: NewsArticle) {
        viewModelScope.launch {
            try {
                repository.deleteArticle(article)
                _uiState.value = _uiState.value.copy(
                    successMessage = "뉴스가 삭제되었습니다."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "삭제 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun deleteAllNews() {
        viewModelScope.launch {
            try {
                repository.deleteAllNews()
                _uiState.value = _uiState.value.copy(
                    successMessage = "모든 뉴스가 삭제되었습니다."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "삭제 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearSelectedArticle() {
        _uiState.value = _uiState.value.copy(selectedArticle = null)
    }
    
    fun toggleFavorite(article: NewsArticle) {
        viewModelScope.launch {
            try {
                repository.toggleFavorite(article)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "즐겨찾기 처리 중 오류가 발생했습니다: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 기사 전체 본문 가져오기
     */
    suspend fun fetchArticleContent(url: String): String {
        return try {
            repository.getArticleContent(url)
        } catch (e: Exception) {
            "본문을 불러오는 중 오류가 발생했습니다: ${e.message}"
        }
    }
    
    /**
     * 즐겨찾기 기반 뉴스 추천
     */
    fun searchRecommendedNews() {
        viewModelScope.launch {
            val favoriteArticles = savedNews.value.filter { it.isFavorite }
            
            if (favoriteArticles.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "즐겨찾기한 기사가 없어 추천할 수 없습니다."
                )
                return@launch
            }
            
            val recommendationKeyword = NewsRecommendationUtil.generateRecommendationQuery(favoriteArticles)
            
            if (recommendationKeyword != null) {
                val reason = NewsRecommendationUtil.generateRecommendationReason(favoriteArticles, recommendationKeyword)
                println("[NewsViewModel] 추천: $reason")
                
                // 추천 키워드로 검색
                searchNews(recommendationKeyword)
                
                _uiState.value = _uiState.value.copy(
                    successMessage = reason
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "추천할 키워드를 찾을 수 없습니다."
                )
            }
        }
    }
}
