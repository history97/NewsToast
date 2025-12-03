package com.example.newsaisummary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsaisummary.data.model.NewsArticle
import com.example.newsaisummary.ui.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * 뉴스 기사 상세 화면
 * 전체 본문과 요약문을 함께 표시
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: NewsArticle,
    viewModel: NewsViewModel,
    onNavigateBack: () -> Unit
) {
    var isLoadingContent by remember { mutableStateOf(false) }
    var fullContent by remember { mutableStateOf(article.content) }
    var showSummary by remember { mutableStateOf(article.isSummarized) }
    
    // 본문이 비어있으면 자동으로 가져오기
    LaunchedEffect(article.url) {
        if (fullContent.isEmpty() || fullContent == "본문 없음") {
            isLoadingContent = true
            val content = viewModel.fetchArticleContent(article.url)
            fullContent = content
            isLoadingContent = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "기사 상세",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "뒤로가기")
                    }
                },
                actions = {
                    // 즐겨찾기 토글
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(article)
                        }
                    ) {
                        Icon(
                            if (article.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "즐겨찾기",
                            tint = if (article.isFavorite) Color.Red else Color.Gray
                        )
                    }
                    
                    // 공유
                    IconButton(onClick = { /* TODO: 공유 기능 */ }) {
                        Icon(Icons.Default.Share, "공유")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 기사 헤더
            ArticleHeader(article)
            
            Divider()
            
            // 요약 / 전체보기 탭
            if (article.isSummarized) {
                TabRow(
                    selectedTabIndex = if (showSummary) 0 else 1,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = showSummary,
                        onClick = { showSummary = true },
                        text = { Text("요약") }
                    )
                    Tab(
                        selected = !showSummary,
                        onClick = { showSummary = false },
                        text = { Text("전체보기") }
                    )
                }
            }
            
            // 본문 내용
            if (isLoadingContent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "기사 본문을 불러오는 중...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                if (article.isSummarized && showSummary) {
                    // 요약문 표시
                    ArticleContent(
                        title = "AI 요약",
                        content = article.summary,
                        isHighlighted = true
                    )
                } else {
                    // 전체 본문 표시
                    ArticleContent(
                        title = "전체 기사",
                        content = fullContent
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 기사 헤더 (제목, 출처, 날짜)
 */
@Composable
private fun ArticleHeader(article: NewsArticle) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 제목
        Text(
            text = article.title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // 메타 정보
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 카테고리
            if (article.category != "ALL") {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = article.category,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // 날짜
            Text(
                text = formatDate(article.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 원문 링크 버튼
        OutlinedButton(
            onClick = { /* TODO: 브라우저로 열기 */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("원문 보기")
        }
    }
}

/**
 * 기사 본문 내용
 */
@Composable
private fun ArticleContent(
    title: String,
    content: String,
    isHighlighted: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 섹션 제목
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (isHighlighted) Icons.Default.AutoAwesome else Icons.Default.Article,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isHighlighted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isHighlighted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
        
        Divider()
        
        // 본문
        if (content.isEmpty() || content == "본문 없음") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "본문을 불러올 수 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "원문 보기를 통해 기사를 확인해주세요",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 28.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * 날짜 포맷팅
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREAN)
    return dateFormat.format(Date(timestamp))
}
