package com.example.newsaisummary

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newsaisummary.data.api.RetrofitInstance
import com.example.newsaisummary.data.local.NewsDatabase
import com.example.newsaisummary.data.repository.NewsRepository
import com.example.newsaisummary.data.scraper.NewsScraper
import com.example.newsaisummary.ui.screens.NewsScreen
import com.example.newsaisummary.ui.theme.NewsAiSummaryTheme
import com.example.newsaisummary.ui.viewmodel.NewsViewModel
import com.example.newsaisummary.ui.viewmodel.NewsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = NewsDatabase.getDatabase(applicationContext)
        val repository = NewsRepository(
            newsDao = database.newsDao(),
            openAiApi = RetrofitInstance.openAiApi,
            newsScraper = NewsScraper()
        )

        setContent {
            NewsAiSummaryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: NewsViewModel = viewModel(
                        factory = NewsViewModelFactory(repository)
                    )

                    val uiState by viewModel.uiState.collectAsState()
                    val savedNews by viewModel.savedNews.collectAsState()

                    NewsScreen(
                        uiState = uiState,
                        savedNews = savedNews,
                        onFetchNews = { category ->
                            viewModel.fetchLatestNews(category)
                        },
                        onSearchNews = { keyword ->
                            viewModel.searchNews(keyword)
                        },
                        onSummarizeArticle = { newsItem, isTest ->
                            viewModel.summarizeAndSaveArticle(newsItem, isTest)
                        },
                        onDeleteArticle = { article ->
                            viewModel.deleteArticle(article)
                        },
                        onClearError = {
                            viewModel.clearError()
                        },
                        onCategorySelected = { category ->
                            viewModel.selectCategory(category)
                        },
                        onToggleFavorite = { article ->
                            viewModel.toggleFavorite(article)
                        },
                        onArticleClick = { article ->
                            // 외부 브라우저로 원문 열기
                            openInBrowser(article.url)
                        },
                        onRecommendNews = {
                            // 즐겨찾기 기반 추천 뉴스 검색
                            viewModel.searchRecommendedNews()
                        }
                    )
                }
            }
        }
    }
    
    /**
     * 외부 브라우저로 URL 열기
     */
    private fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
