package com.example.newsaisummary.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import com.example.newsaisummary.data.model.NewsArticle
import com.example.newsaisummary.data.model.NewsCategory
import com.example.newsaisummary.data.model.NewsItem
import com.example.newsaisummary.ui.viewmodel.NewsUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    uiState: NewsUiState,
    savedNews: List<NewsArticle>,
    onFetchNews: (NewsCategory) -> Unit,
    onSearchNews: (String) -> Unit,
    onSummarizeArticle: (NewsItem, Boolean) -> Unit,
    onDeleteArticle: (NewsArticle) -> Unit,
    onClearError: () -> Unit,
    onCategorySelected: (NewsCategory) -> Unit,
    onToggleFavorite: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onRecommendNews: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("최신 뉴스", "저장된 요약", "즐겨찾기")  // 탭 3개로 확장
    
    var searchQuery by remember { mutableStateOf("") }
    
    // 추천 뉴스 검색 시 최신 뉴스 탭으로 전환
    val handleRecommendNews: () -> Unit = {
        selectedTab = 0  // 최신 뉴스 탭으로 전환
        onRecommendNews()  // 추천 검색 실행
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 뉴스 요약", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE3F2FD)
                        )
                    )
                )
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.titleMedium) },
                        icon = {
                            Icon(
                                when (index) {
                                    0 -> Icons.Default.Newspaper
                                    1 -> Icons.Default.Bookmark
                                    else -> Icons.Default.Favorite
                                },
                                contentDescription = title
                            )
                        }
                    )
                }
            }

            CategoryFilter(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = onCategorySelected
            )

            when (selectedTab) {
                0 -> {
                    LatestNewsTab(
                        uiState = uiState,
                        onFetchNews = onFetchNews,
                        onSearchNews = onSearchNews,
                        onSummarizeArticle = onSummarizeArticle,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it }
                    )
                }
                1 -> {
                    SavedNewsTab(
                        savedNews = savedNews,
                        selectedCategory = uiState.selectedCategory,
                        onDeleteArticle = onDeleteArticle,
                        onToggleFavorite = onToggleFavorite,
                        onArticleClick = onArticleClick
                    )
                }
                2 -> {
                    FavoriteNewsTab(
                        savedNews = savedNews,
                        selectedCategory = uiState.selectedCategory,
                        onDeleteArticle = onDeleteArticle,
                        onToggleFavorite = onToggleFavorite,
                        onArticleClick = onArticleClick,
                        onRecommendNews = handleRecommendNews
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            uiState.errorMessage?.let { error ->
                Snackbar(
                    action = { TextButton(onClick = onClearError) { Text("확인") } },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) { Text(error) }
            }
        }
    }
}

@Composable
fun SearchFilter(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("키워드 검색") },
            placeholder = { Text("검색어 입력...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "검색어 지우기")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                focusedLabelColor = Color(0xFF1976D2),
                unfocusedTextColor = Color(0xFF212121),  // 비활성 상태 텍스트 색 (검은색)
                focusedTextColor = Color(0xFF000000),     // 활성 상태 텍스트 색 (진한 검은색)
                cursorColor = Color(0xFF1976D2)          // 커서 색
            )
        )
    }
}

@Composable
fun CategoryFilter(selectedCategory: NewsCategory, onCategorySelected: (NewsCategory) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(NewsCategory.values()) { category ->
            CategoryChip(category, category == selectedCategory) { onCategorySelected(category) }
        }
    }
}

@Composable
fun CategoryChip(category: NewsCategory, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) Color(category.color) else Color(0xFFF0F0F0),
        contentColor = if (isSelected) Color.White else Color(0xFF666666),
        shadowElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Text(
            text = category.displayName,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun LatestNewsTab(
    uiState: NewsUiState,
    onFetchNews: (NewsCategory) -> Unit,
    onSearchNews: (String) -> Unit,
    onSummarizeArticle: (NewsItem, Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    var lastSearchedQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchFilter(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    lastSearchedQuery = ""
                    onFetchNews(uiState.selectedCategory)
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingNews,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                if (uiState.isLoadingNews && lastSearchedQuery.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("최신 뉴스", style = MaterialTheme.typography.labelMedium)
            }

            Button(
                onClick = {
                    if (searchQuery.isNotEmpty()) {
                        lastSearchedQuery = searchQuery
                        Log.d("LatestNewsTab", "검색: '$lastSearchedQuery'")
                        onSearchNews(searchQuery)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isLoadingNews && searchQuery.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0288D1)
                )
            ) {
                if (uiState.isLoadingNews && lastSearchedQuery.isNotEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("검색", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (uiState.latestNews.isNotEmpty() && !uiState.isLoadingNews) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = if (lastSearchedQuery.isNotEmpty()) Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (lastSearchedQuery.isNotEmpty()) Icons.Default.Search else Icons.Default.Newspaper,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (lastSearchedQuery.isNotEmpty()) Color(0xFF1976D2) else Color(0xFFF57C00)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (lastSearchedQuery.isNotEmpty()) "검색 결과 (관련도순)" else "최신 헤드라인",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF333333)
                        )
                        Text(
                            if (lastSearchedQuery.isNotEmpty()) {
                                "'$lastSearchedQuery' 관련 기사 ${uiState.latestNews.size}개"
                            } else {
                                "${uiState.selectedCategory.displayName} ${uiState.latestNews.size}개"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }

        if (uiState.isLoadingContent || uiState.isSummarizing) {
            LoadingIndicator(
                when {
                    uiState.isLoadingContent -> "본문을 가져오는 중..."
                    uiState.isSummarizing -> "AI가 요약하는 중..."
                    else -> "처리 중..."
                }
            )
        }

        if (uiState.latestNews.isEmpty() && !uiState.isLoadingNews) {
            EmptyState(
                Icons.Default.Newspaper,
                if (lastSearchedQuery.isNotEmpty()) {
                    "'$lastSearchedQuery' 검색 결과가 없습니다"
                } else {
                    "최신 뉴스 버튼을 눌러\n${uiState.selectedCategory.displayName} 뉴스를 가져오세요"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.latestNews) { newsItem ->
                    NewsItemCard(
                        newsItem = newsItem,
                        onSummarize = { isTest -> onSummarizeArticle(newsItem, isTest) },
                        isDisabled = uiState.isLoadingContent || uiState.isSummarizing
                    )
                }
            }
        }
    }
}


@Composable
fun SavedNewsTab(
    savedNews: List<NewsArticle>,
    selectedCategory: NewsCategory,
    onDeleteArticle: (NewsArticle) -> Unit,
    onToggleFavorite: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit
) {
    val filteredNews = remember(savedNews, selectedCategory) {
        if (selectedCategory == NewsCategory.ALL) {
            savedNews
        } else {
            savedNews.filter { it.category == selectedCategory.name }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (filteredNews.isEmpty()) {
            EmptyState(
                Icons.Default.BookmarkBorder,
                if (selectedCategory != NewsCategory.ALL) {
                    "${selectedCategory.displayName} 카테고리에\n저장된 뉴스가 없습니다"
                } else {
                    "저장된 뉴스가 없습니다\n뉴스를 요약하면 여기에 저장됩니다"
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNews, key = { it.id }) { article ->
                    SavedArticleCard(
                        article = article,
                        onDelete = { onDeleteArticle(article) },
                        onToggleFavorite = { onToggleFavorite(article) },
                        onArticleClick = { onArticleClick(article) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteNewsTab(
    savedNews: List<NewsArticle>,
    selectedCategory: NewsCategory,
    onDeleteArticle: (NewsArticle) -> Unit,
    onToggleFavorite: (NewsArticle) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onRecommendNews: () -> Unit
) {
    // 즐겨찾기만 필터링
    val favoriteNews = remember(savedNews, selectedCategory) {
        val filtered = if (selectedCategory == NewsCategory.ALL) {
            savedNews.filter { it.isFavorite }
        } else {
            savedNews.filter { it.isFavorite && it.category == selectedCategory.name }
        }
        filtered
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (favoriteNews.isEmpty()) {
            EmptyState(
                Icons.Default.FavoriteBorder,
                if (selectedCategory != NewsCategory.ALL) {
                    "${selectedCategory.displayName} 카테고리에\n즐겨찾기한 뉴스가 없습니다"
                } else {
                    "즐겨찾기한 뉴스가 없습니다\n다이아몬드 하트를 눌러 즐겨찾기를 추가하세요"
                }
            )
        } else {
            // 즐겨찾기 개수 표시 및 추천 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFF1565C0)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "즐겨찾기 ${favoriteNews.size}개",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF333333)
                        )
                    }
                }
                
                Button(
                    onClick = onRecommendNews,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "관련 뉴스 찾기",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favoriteNews, key = { it.id }) { article ->
                    SavedArticleCard(
                        article = article,
                        onDelete = { onDeleteArticle(article) },
                        onToggleFavorite = { onToggleFavorite(article) },
                        onArticleClick = { onArticleClick(article) }
                    )
                }
            }
        }
    }
}

@Composable
fun NewsItemCard(
    newsItem: NewsItem,
    onSummarize: (Boolean) -> Unit,
    isDisabled: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(newsItem.category.color).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = newsItem.category.displayName,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(newsItem.category.color)
                    )
                }
                // 발행 날짜 표시
                if (newsItem.publishDate.isNotEmpty()) {
                    Text(
                        text = newsItem.publishDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = newsItem.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // 요약하기 버튼만 표시 (체크박스 제거)
            Button(
                onClick = { onSummarize(false) },  // 항상 false (일반 모드)
                modifier = Modifier.align(Alignment.End),
                enabled = !isDisabled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                )
            ) {
                Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("요약하기")
            }
        }
    }
}

@Composable
fun SavedArticleCard(
    article: NewsArticle,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArticleClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }  // 삭제 확인 다이얼로그
    
    val category = try {
        NewsCategory.valueOf(article.category)
    } catch (e: Exception) {
        NewsCategory.ALL
    }
    
    // [test] 접두사 처리
    val displayTitle = if (article.isTest && !article.title.startsWith("[test]")) {
        "[test] ${article.title}"
    } else {
        article.title
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "삭제 확인",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = "이 기사를 삭제하시겠습니까?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("취소")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(category.color).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = category.displayName,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(category.color)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 다이아몬드 하트 즐겨찾기 아이콘
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(40.dp)
                    ) {
                        DiamondHeartIcon(
                            isFavorite = article.isFavorite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },  // 다이얼로그 표시
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = if (article.isTest) Color(0xFF666666) else Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatTimestamp(article.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF999999)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = Color(0xFFF5F9FF),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AI 요약",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1976D2)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF333333),
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF1976D2)
                    )
                ) {
                    Text(
                        if (expanded) "접기" else "요약 더보기",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Button(
                    onClick = onArticleClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Icon(
                        Icons.Default.Article,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "전체 기사",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

// 다이아몬드 하트 아이콘 Composable
@Composable
fun DiamondHeartIcon(
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    Icon(
        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기",
        tint = if (isFavorite) Color(0xFF1565C0) else Color(0xFFBDBDBD),  // 진한 파란색
        modifier = modifier.rotate(45f)
    )
}

@Composable
fun LoadingIndicator(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFFF5F9FF),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp), 
                strokeWidth = 3.dp,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFBDBDBD)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF999999),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
