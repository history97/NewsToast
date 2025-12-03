package com.example.newsaisummary.data.scraper

import com.example.newsaisummary.BuildConfig
import com.example.newsaisummary.data.api.RetrofitInstance
import com.example.newsaisummary.data.model.NewsCategory
import com.example.newsaisummary.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class NewsScraper {

    // 필터링할 불필요한 문구 목록
    private val filterKeywords = listOf(
        "언론사 선정",
        "언론사가 선정한",
        "주요기사 혹은 심충기획",
        "네이버 메인에서 보고 싶은 언론사를 구독하세요",
        "더보기",
        "전체보기"
    )
    
    /**
     * 필터링이 필요한 제목인지 확인
     */
    private fun shouldFilterTitle(title: String): Boolean {
        return filterKeywords.any { keyword -> title.contains(keyword) }
    }

    /**
     * 네이버 뉴스 헤드라인 크롤링 (실제 뉴스)
     */
    suspend fun scrapeNaverNews(category: NewsCategory = NewsCategory.ALL): List<NewsItem> =
        withContext(Dispatchers.IO) {
            try {
                println("[NewsScraper] 실제 뉴스 크롤링 시작: ${category.displayName}")
                
                val urls = if (category == NewsCategory.ALL) {
                    listOf(
                        "https://news.naver.com/section/100" to NewsCategory.POLITICS,
                        "https://news.naver.com/section/101" to NewsCategory.ECONOMY,
                        "https://news.naver.com/section/102" to NewsCategory.SOCIETY,
                        "https://news.naver.com/section/103" to NewsCategory.LIFE,
                        "https://news.naver.com/section/105" to NewsCategory.IT
                    )
                } else {
                    val sectionId = category.naverSectionId ?: "100"
                    listOf("https://news.naver.com/section/$sectionId" to category)
                }

                val allNews = mutableListOf<NewsItem>()
                val newsPerCategory = if (category == NewsCategory.ALL) 5 else 20

                for ((url, targetCategory) in urls) {
                    try {
                        println("[NewsScraper] 크롤링 시도: $url (${targetCategory.displayName})")
                        
                        // 실제 뉴스 크롤링 시도
                        val realNews = fetchRealNews(url, targetCategory, newsPerCategory)
                        
                        if (realNews.isNotEmpty()) {
                            println("[NewsScraper] ✅ ${targetCategory.displayName}: 실제 뉴스 ${realNews.size}개 수집")
                            allNews.addAll(realNews)
                        } else {
                            // 실패시에만 테스트 뉴스 제공
                            println("[NewsScraper] ⚠️ ${targetCategory.displayName}: 실제 뉴스 수집 실패, 테스트 뉴스로 대체")
                            val testNews = generateSampleNews(targetCategory, newsPerCategory)
                            allNews.addAll(testNews)
                        }
                    } catch (e: Exception) {
                        println("[NewsScraper] ❌ 크롤링 실패: $url - ${e.message}")
                        // 오류 발생시 테스트 뉴스로 대체
                        val testNews = generateSampleNews(targetCategory, newsPerCategory)
                        allNews.addAll(testNews)
                    }
                }

                println("[NewsScraper] 총 ${allNews.size}개 뉴스 수집 완료")
                allNews
            } catch (e: Exception) {
                println("[NewsScraper] 전체 실패: ${e.message}")
                e.printStackTrace()
                // 전체 실패시 테스트 뉴스 제공
                generateSampleNews(category, 10)
            }
        }

    /**
     * 실제 네이버 뉴스 크롤링
     */
    private fun fetchRealNews(url: String, category: NewsCategory, maxNews: Int): List<NewsItem> {
        return try {
            println("[NewsScraper] HTML 가져오기 시작: $url")
            
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .referrer("https://www.naver.com")
                .timeout(15000)
                .get()

            println("[NewsScraper] HTML 가져오기 성공")

            // 네이버 뉴스 섹션 페이지의 다양한 선택자 시도
            val selectors = listOf(
                // 메인 헤드라인
                "div.sa_text > a.sa_text_title",
                // 뉴스 리스트
                "ul.sa_list > li > div > a",
                "ul.sa_list a[href*='/article/']",
                // 일반적인 뉴스 링크
                "a[href*='/article/']",
                // 섹션별 뉴스
                "div.section_latest a.sa_text_title",
                "div.cluster_text > a",
                // 추가 선택자
                "div.sa_item a.sa_text_title",
                "li.sa_item a[href*='/article/']"
            )

            val newsList = mutableListOf<NewsItem>()
            var foundWithSelector: String? = null
            
            for (selector in selectors) {
                val elements = doc.select(selector)
                println("[NewsScraper] 선택자 '$selector': ${elements.size}개 발견")
                
                if (elements.size > 0) {
                    foundWithSelector = selector
                    
                    elements.take(maxNews).forEachIndexed { index, el ->
                        try {
                            val title = el.text().trim()
                            var link = el.attr("href")
                            
                            if (link.startsWith("/")) {
                                link = "https://news.naver.com$link"
                            }

                            val isValid = link.contains("/article/") && 
                                         title.length > 10 && 
                                         !shouldFilterTitle(title) &&
                                         !title.contains("더보기") &&
                                         !title.contains("전체보기")

                            if (isValid && newsList.none { it.url == link }) {
                                // 날짜 정보 추출 시도
                                val parent = el.parent()
                                val dateText = parent?.select("span.sa_text_datetime, span.sa_time")?.text() ?: ""
                                val publishDate = extractDateFromText(dateText)
                                
                                // 언론사 정보 추출
                                val press = parent?.select("div.sa_text_press, span.sa_text_press")?.text()?.trim() ?: ""
                                val finalTitle = if (press.isNotEmpty()) "[$press] $title" else title
                                
                                newsList.add(
                                    NewsItem(
                                        title = finalTitle,
                                        url = link,
                                        content = "",
                                        category = category,
                                        publishDate = publishDate
                                    )
                                )
                                println("[NewsScraper] [$index] 추가: $title")
                            }
                        } catch (e: Exception) {
                            println("[NewsScraper] [$index] 파싱 오류: ${e.message}")
                        }
                    }
                    
                    if (newsList.isNotEmpty()) {
                        println("[NewsScraper] ✅ 선택자 '$selector'로 ${newsList.size}개 뉴스 수집 성공")
                        break
                    }
                }
            }
            
            // 모든 선택자로도 못 찾은 경우, 모든 링크 분석
            if (newsList.isEmpty()) {
                println("[NewsScraper] ⚠️ 선택자로 못찾음, 전체 링크 분석 시작")
                
                val allLinks = doc.select("a[href*='/article/']")
                println("[NewsScraper] 전체 기사 링크: ${allLinks.size}개")
                
                allLinks.take(maxNews).forEachIndexed { index, link ->
                    try {
                        val title = link.text().trim()
                        var href = link.attr("href")
                        
                        if (href.startsWith("/")) {
                            href = "https://news.naver.com$href"
                        }
                        
                        if (title.length > 10 && 
                            !shouldFilterTitle(title) &&
                            !title.contains("더보기") && 
                            !title.contains("전체보기") &&
                            newsList.none { it.url == href }) {
                            
                            val publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(System.currentTimeMillis())
                            
                            newsList.add(
                                NewsItem(
                                    title = title,
                                    url = href,
                                    content = "",
                                    category = category,
                                    publishDate = publishDate
                                )
                            )
                            println("[NewsScraper] [대체-$index] 추가: $title")
                        }
                    } catch (e: Exception) {
                        println("[NewsScraper] [대체-$index] 오류: ${e.message}")
                    }
                }
            }
            
            if (newsList.isEmpty()) {
                println("[NewsScraper] ❌ 실제 뉴스 수집 완전 실패")
            }
            
            newsList
        } catch (e: Exception) {
            println("[NewsScraper] HTML 크롤링 오류: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 네이버 API를 사용한 뉴스 검색 (권장)
     * 검색어 포함 기사를 정확도순으로 가져온다
     */
    suspend fun searchNews(
        keyword: String,
        maxResults: Int = 30,
        sortByDate: Boolean = false  // false: 정확도순, true: 최신순
    ): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            if (keyword.isBlank()) {
                println("[NewsScraper-API검색] 오류: 검색어가 비어있음")
                return@withContext emptyList()
            }

            println("[NewsScraper-API검색] 네이버 API 검색 시작: keyword='$keyword', sort=${if (sortByDate) "최신순" else "정확도순"}")
            
            val response = RetrofitInstance.naverNewsApi.searchNews(
                query = keyword,
                display = maxResults.coerceAtMost(100),
                sort = if (sortByDate) "date" else "sim",  // sim: 정확도순, date: 최신순
                clientId = BuildConfig.NAVER_CLIENT_ID,
                clientSecret = BuildConfig.NAVER_CLIENT_SECRET
            )
            
            println("[NewsScraper-API검색] API 응답: total=${response.total}, items=${response.items.size}")
            
            val newsList = response.items.mapNotNull { item ->
                try {
                    // HTML 태그 제거
                    val cleanTitle = item.title
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                    
                    val cleanDescription = item.description
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                    
                    // URL 우선순위: originallink > link
                    val newsUrl = item.originallink.ifEmpty { item.link }
                    
                    // 언론사 추출
                    val source = extractSourceFromUrl(newsUrl)
                    
                    // 날짜 파싱
                    val publishDate = parseNaverApiDate(item.pubDate)
                    
                    // 카테고리 추출
                    val category = extractCategoryFromUrl(newsUrl)
                    
                    NewsItem(
                        title = cleanTitle,
                        url = newsUrl,
                        content = cleanDescription,
                        category = category,
                        publishDate = publishDate
                    )
                } catch (e: Exception) {
                    println("[NewsScraper-API검색] 아이템 파싱 에러: \${e.message}")
                    null
                }
            }
            
            println("[NewsScraper-API검색] ✅ 최종 결과: ${newsList.size}개")
            newsList
            
        } catch (e: Exception) {
            println("[NewsScraper-API검색] ❌ API 에러: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 키워드로 네이버 뉴스 검색 (크롤링 방식 - 백업용)
     * 검색어 포함 기사를 가져온다 (정확도순)
     */
    suspend fun searchNaverNews(
        keyword: String,
        maxResults: Int = 30,
        sortByDate: Boolean = false  // false: 관련도순, true: 최신순
    ): List<NewsItem> = withContext(Dispatchers.IO) {
        try {
            if (keyword.isBlank()) {
                println("[NewsScraper-검색] 오류: 검색어가 비어있음")
                return@withContext emptyList()
            }

            println("[NewsScraper-검색] 실제 검색 시작: keyword='$keyword', sort=${if (sortByDate) "최신순" else "관련도순"}")
            
            // 네이버 뉴스 검색 URL
            val encodedKeyword = java.net.URLEncoder.encode(keyword, "UTF-8")
            // sort=0: 관련도순, sort=1: 최신순
            val sortParam = if (sortByDate) "1" else "0"
            val searchUrl = "https://search.naver.com/search.naver?where=news&query=$encodedKeyword&sort=$sortParam&photo=0&field=0&pd=0&ds=&de=&docid=&related=0&mynews=0&office_type=0&office_section_code=0&news_office_checked=&nso=so:${if (sortByDate) "r" else "dd"},p:all,a:all"
            
            println("[NewsScraper-검색] URL: $searchUrl")

            val doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .referrer("https://www.naver.com")
                .timeout(15000)
                .get()

            println("[NewsScraper-검색] HTML 가져오기 성공")

            val newsList = mutableListOf<NewsItem>()
            
            // 네이버 검색 결과 선택자들
            val newsSelectors = listOf(
                "div.news_wrap",
                "div.group_news > ul > li",
                "ul.list_news > li",
                "div.news_area",
                "li.bx",
                "div.api_subject_bx"
            )
            
            var foundAny = false
            
            for (selector in newsSelectors) {
                val elements = doc.select(selector)
                println("[NewsScraper-검색] 선택자 '$selector': ${elements.size}개 발견")
                
                if (elements.size > 0) {
                    foundAny = true
                    
                    elements.take(maxResults).forEachIndexed { index, element ->
                        try {
                            // 제목과 링크 추출
                            val titleElement = element.select("a.news_tit, a.title, a[href*='article'], a[href*='read']").first()
                            
                            val title = titleElement?.text()?.trim() ?: ""
                            var url = titleElement?.attr("href") ?: ""
                            
                            if (url.startsWith("/")) {
                                url = "https://search.naver.com$url"
                            }
                            
                            // 날짜 추출
                            val dateElement = element.select("span.info, span.txt_inline").firstOrNull { 
                                it.text().contains("시간") || it.text().contains("분") || it.text().contains("일") || it.text().matches(Regex("\\d{4}\\.\\d{2}\\.\\d{2}.*"))
                            }
                            val dateText = dateElement?.text() ?: ""
                            val publishDate = extractDateFromText(dateText)
                            
                            // 언론사 추출
                            val press = element.select("a.info.press, a.press, span.press").text().trim()
                            
                            val isValidNews = title.length > 10 && 
                                             url.isNotEmpty() && 
                                             (url.contains("naver.com") || url.contains("news")) &&
                                             !shouldFilterTitle(title)
                            
                            if (isValidNews && newsList.none { it.url == url }) {
                                val category = extractCategoryFromUrl(url)
                                val finalTitle = if (press.isNotEmpty()) "[$press] $title" else title
                                
                                val newsItem = NewsItem(
                                    title = finalTitle,
                                    url = url,
                                    content = "",
                                    category = category,
                                    publishDate = publishDate
                                )
                                
                                newsList.add(newsItem)
                                println("[NewsScraper-검색] [$index] 추가: $title")
                            }
                        } catch (e: Exception) {
                            println("[NewsScraper-검색] [$index] 파싱 실패: ${e.message}")
                        }
                    }
                    
                    if (newsList.isNotEmpty()) {
                        println("[NewsScraper-검색] ✅ 선택자 '$selector'로 ${newsList.size}개 검색 결과 수집")
                        break
                    }
                }
            }

            // 대체 방법: 모든 뉴스 링크 찾기
            if (newsList.isEmpty()) {
                println("[NewsScraper-검색] ⚠️ 선택자로 찾기 실패, 모든 링크 분석")
                
                val allLinks = doc.select("a[href*='article'], a[href*='read'], a[href*='news']")
                println("[NewsScraper-검색] 전체 뉴스 링크: ${allLinks.size}개")
                
                allLinks.take(maxResults).forEachIndexed { index, link ->
                    try {
                        val title = link.text().trim()
                        var url = link.attr("href")
                        
                        if (url.startsWith("/")) {
                            url = "https://search.naver.com$url"
                        }
                        
                        if (title.length > 10 && 
                            !shouldFilterTitle(title) &&
                            !title.contains("더보기") &&
                            newsList.none { it.url == url }) {
                            
                            val category = extractCategoryFromUrl(url)
                            val publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                                .format(System.currentTimeMillis())
                            
                            newsList.add(NewsItem(title, url, "", category, publishDate))
                            println("[NewsScraper-검색] [대체-$index] 추가: $title")
                        }
                    } catch (e: Exception) {
                        println("[NewsScraper-검색] [대체-$index] 실패: ${e.message}")
                    }
                }
            }

            // 검색 실패시 테스트 뉴스 제공
            if (newsList.isEmpty()) {
                println("[NewsScraper-검색] ❌ 실제 검색 실패, 테스트 뉴스 제공")
                return@withContext listOf(
                    NewsItem(
                        title = "[test] '$keyword' 검색 테스트 기사 1",
                        url = "https://news.naver.com/test/search/1",
                        content = "",
                        category = NewsCategory.ALL,
                        publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(System.currentTimeMillis())
                    ),
                    NewsItem(
                        title = "[test] '$keyword' 관련 테스트 기사 2",
                        url = "https://news.naver.com/test/search/2",
                        content = "",
                        category = NewsCategory.ALL,
                        publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(System.currentTimeMillis())
                    )
                )
            }

            println("[NewsScraper-검색] ✅ 최종 검색 결과: ${newsList.size}개")
            newsList
            
        } catch (e: Exception) {
            println("[NewsScraper-검색] 전체 실패: ${e.message}")
            e.printStackTrace()
            // 오류시 테스트 뉴스 제공
            listOf(
                NewsItem(
                    title = "[test] 검색 오류 - 테스트 기사",
                    url = "https://news.naver.com/test/error",
                    content = "",
                    category = NewsCategory.ALL,
                    publishDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(System.currentTimeMillis())
                )
            )
        }
    }

    /**
     * 네이버 API 날짜 형식 파싱
     * 예: "Mon, 27 Nov 2025 14:30:00 +0900"
     */
    private fun parseNaverApiDate(dateStr: String): String {
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
            val date = format.parse(dateStr)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            println("[NewsScraper] 날짜 파싱 에러: $dateStr - ${e.message}")
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(System.currentTimeMillis())
        }
    }
    
    /**
     * URL에서 언론사 이름 추출
     */
    private fun extractSourceFromUrl(url: String): String {
        return try {
            val domain = url.substringAfter("://").substringBefore("/")
            when {
                domain.contains("chosun") -> "조선일보"
                domain.contains("joongang") -> "중앙일보"
                domain.contains("donga") -> "동아일보"
                domain.contains("hankyung") -> "한국경제"
                domain.contains("khan") -> "경향신문"
                domain.contains("hani") -> "한겨레"
                domain.contains("sbs") -> "SBS"
                domain.contains("kbs") -> "KBS"
                domain.contains("mbc") -> "MBC"
                domain.contains("jtbc") -> "JTBC"
                domain.contains("ytn") -> "YTN"
                domain.contains("yna") || domain.contains("yonhap") -> "연합뉴스"
                domain.contains("newsis") -> "뉴시스"
                domain.contains("news1") -> "뉴스1"
                domain.contains("mt.co.kr") -> "머니투데이"
                domain.contains("mk.co.kr") -> "매일경제"
                domain.contains("sedaily") -> "서울경제"
                domain.contains("edaily") -> "이데일리"
                domain.contains("heraldcorp") -> "헤럴드경제"
                else -> domain.substringBefore(".").replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
            }
        } catch (e: Exception) {
            "알 수 없음"
        }
    }
    
    /**
     * 날짜 텍스트에서 날짜 추출
     */
    private fun extractDateFromText(dateText: String): String {
        return try {
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            
            when {
                dateText.contains("시간 전") || dateText.contains("분 전") -> {
                    dateFormat.format(now)
                }
                dateText.contains("일 전") -> {
                    val days = dateText.replace("일 전", "").trim().toIntOrNull() ?: 0
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -days)
                    dateFormat.format(calendar.time)
                }
                dateText.matches(Regex("\\d{4}\\.\\d{2}\\.\\d{2}.*")) -> {
                    dateText.substring(0, 10).replace(".", "-")
                }
                else -> {
                    dateFormat.format(now)
                }
            }
        } catch (e: Exception) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(System.currentTimeMillis())
        }
    }

    /**
     * URL에서 카테고리 추출
     */
    private fun extractCategoryFromUrl(url: String): NewsCategory {
        return when {
            url.contains("/section/100") || url.contains("sid=100") -> NewsCategory.POLITICS
            url.contains("/section/101") || url.contains("sid=101") -> NewsCategory.ECONOMY
            url.contains("/section/102") || url.contains("sid=102") -> NewsCategory.SOCIETY
            url.contains("/section/103") || url.contains("sid=103") -> NewsCategory.LIFE
            url.contains("/section/104") || url.contains("sid=104") -> NewsCategory.WORLD
            url.contains("/section/105") || url.contains("sid=105") -> NewsCategory.IT
            else -> NewsCategory.ALL
        }
    }

    /**
     * 테스트용 샘플 뉴스 생성 (실제 크롤링 실패시에만 사용)
     */
    private fun generateSampleNews(category: NewsCategory, count: Int): List<NewsItem> {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = dateFormat.format(System.currentTimeMillis())
        
        val sampleTitles = when (category) {
            NewsCategory.POLITICS -> listOf(
                "정부, 새로운 경제 정책 발표 예정",
                "국회, 예산안 논의 본격화",
                "외교부, 주요 국가와 협력 강화 방침"
            )
            NewsCategory.ECONOMY -> listOf(
                "기업, 신규 투자 계획 발표",
                "증시, 외국인 매수세 지속",
                "환율, 글로벌 경제 동향에 민감 반응"
            )
            NewsCategory.SOCIETY -> listOf(
                "교육부, 새 학기 준비 지침사항 발표",
                "경찰, 범죄 예방 활동 강화",
                "보건당국, 건강 검진 확대 방침"
            )
            NewsCategory.LIFE -> listOf(
                "날씨, 주말 따뜻한 날씨 예상",
                "건강 팁, 가을철 환절기 대비법",
                "맛집 탐방, SNS에서 화제의 레스토랑"
            )
            NewsCategory.WORLD -> listOf(
                "미국, 새로운 정책 발표",
                "유럽, 경제 협력 회의 개최",
                "중국, 주요 도시 발전 계획 공개"
            )
            NewsCategory.IT -> listOf(
                "AI 기술, 새로운 혁신 돌파구",
                "스마트폰 신제품, 주요 기능 공개",
                "클라우드 서비스, 시장 점유율 확대"
            )
            else -> listOf("샘플 뉴스 제목")
        }
        
        return sampleTitles.take(count).mapIndexed { index, title ->
            NewsItem(
                title = "[test] $title",
                url = "https://news.naver.com/test/${category.name.lowercase()}/$index",
                content = "",
                category = category,
                publishDate = today
            )
        }
    }
    
    /**
     * 기사 본문 크롤링
     */
    suspend fun scrapeArticleContent(url: String): String = withContext(Dispatchers.IO) {
        try {
            println("[NewsScraper] 본문 크롤링 시작: $url")
            
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                .referrer("https://www.naver.com")
                .timeout(15000)
                .get()
            
            println("[NewsScraper] 본문 HTML 가져오기 성공")
            
            // 네이버 뉴스 본문 선택자들
            val selectors = listOf(
                "#dic_area",                    // 가장 일반적
                "#articeBody",                  // 구버전
                "article#dic_area",             // article 태그
                "div#articleBodyContents",      // 다른 형식
                ".article_body",                // 클래스명
                "#newsEndContents",             // 엔딩 영역
                ".end_body_wrp",               // 본문 래퍼
                "div.article_view"              // 기사 뷰
            )
            
            for (selector in selectors) {
                val content = doc.select(selector).text().trim()
                if (content.length > 100) {
                    println("[NewsScraper] ✅ 본문 추출 성공 (${content.length}자)")
                    return@withContext content
                }
            }
            
            // 대체 방법: 모든 p 태그 수집
            println("[NewsScraper] ⚠️ 선택자로 못찾음, p 태그 수집")
            val paragraphs = doc.select("p").joinToString("\n") { it.text() }.trim()
            
            if (paragraphs.length > 100) {
                println("[NewsScraper] ✅ p 태그로 본문 추출 (${paragraphs.length}자)")
                return@withContext paragraphs
            }
            
            println("[NewsScraper] ❌ 본문 추출 실패")
            "본문을 가져올 수 없습니다. 원문 링크를 확인해주세요."
            
        } catch (e: Exception) {
            println("[NewsScraper] 본문 크롤링 오류: ${e.message}")
            e.printStackTrace()
            "본문 크롤링 오류: ${e.message}"
        }
    }
}
