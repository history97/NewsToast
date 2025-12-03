// LatestNewsTab 함수 수정본
// NewsScreen.kt 파일의 LatestNewsTab 함수를 이 코드로 교체하세요

@Composable
fun LatestNewsTab(
    uiState: NewsUiState,
    onFetchNews: (NewsCategory) -> Unit,
    onSearchNews: (String) -> Unit,
    onSummarizeArticle: (NewsItem, Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    // 마지막으로 검색한 키워드 저장 (버튼 클릭 시에만 업데이트)
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
                    lastSearchedQuery = "" // 최신 뉴스는 검색어 초기화
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
                        lastSearchedQuery = searchQuery // 검색 시 키워드 저장
                        Log.d("LatestNewsTab", "검색: '$searchQuery'")
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

        // UI는 lastSearchedQuery 기준으로 표시 (버튼 클릭 시에만 변경됨)
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
                            if (lastSearchedQuery.isNotEmpty()) "검색 결과" else "최신 헤드라인",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF333333)
                        )
                        Text(
                            if (lastSearchedQuery.isNotEmpty()) {
                                "'$lastSearchedQuery' ${uiState.latestNews.size}개"
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
