package com.example.newsaisummary.utils

import com.example.newsaisummary.data.model.NewsArticle

/**
 * 뉴스 추천 유틸리티
 * 즐겨찾기한 기사들을 분석하여 관련 키워드 추출
 */
object NewsRecommendationUtil {
    
    // 한국어 불용어 (의미 없는 단어들)
    private val stopWords = setOf(
        "이", "그", "저", "것", "수", "등", "및", "의", "를", "을", "가", "이", "에", "와", "과",
        "도", "으로", "로", "에서", "부터", "까지", "에게", "한", "할", "하다", "있다", "되다",
        "하는", "있는", "되는", "될", "된", "는", "은", "가", "이", "을", "를", "에", "의",
        "기자", "뉴스", "오늘", "내일", "어제", "오전", "오후", "지난", "다음", "이번",
        "관련", "통해", "대한", "위한", "따른", "위해", "대해", "관한", "같은", "이런", "저런",
        "위원회", "협회", "본부", "센터", "재단", "연구소"
    )
    
    // 언론사 필터링 (일반적인 언론사명 패턴)
    private val mediaPatterns = listOf(
        "일보", "신문", "타임즈", "데일리", "뉴스", "저널", "포스트", "헤럴드",
        "투데이", "매일", "중앙", "동아", "조선", "한겨레", "경향",
        "TV", "방송", "미디어", "신문사"
    )
    
    /**
     * 언론사명인지 확인
     */
    private fun isMediaName(word: String): Boolean {
        return mediaPatterns.any { pattern -> word.contains(pattern) }
    }
    
    /**
     * 즐겨찾기 기사들에서 핵심 키워드 추출
     */
    fun extractKeywordsFromFavorites(favoriteArticles: List<NewsArticle>): List<String> {
        if (favoriteArticles.isEmpty()) return emptyList()
        
        // 모든 즐겨찾기 기사의 제목을 합침
        val allTitles = favoriteArticles.joinToString(" ") { it.title }
        
        // 단어 빈도 분석
        val wordFrequency = mutableMapOf<String, Int>()
        
        // 제목을 단어로 분리 (공백, 특수문자 기준)
        val words = allTitles
            .replace(Regex("[^가-힣a-zA-Z0-9\\s]"), " ") // 특수문자 제거
            .split(Regex("\\s+")) // 공백으로 분리
            .filter { it.length >= 2 } // 2글자 이상만
            .filter { !stopWords.contains(it) } // 불용어 제거
        
        // 빈도수 계산 (언론사명 제외)
        words.forEach { word ->
            if (!isMediaName(word)) {
                wordFrequency[word] = wordFrequency.getOrDefault(word, 0) + 1
            }
        }
        
        // 빈도수 순으로 정렬하고 상위 5개 추출
        return wordFrequency
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }
    
    /**
     * 카테고리별 선호도 분석
     */
    fun getMostFavoriteCategory(favoriteArticles: List<NewsArticle>): String? {
        if (favoriteArticles.isEmpty()) return null
        
        return favoriteArticles
            .groupBy { it.category }
            .maxByOrNull { it.value.size }
            ?.key
    }
    
    /**
     * 추천 검색어 생성
     */
    fun generateRecommendationQuery(favoriteArticles: List<NewsArticle>): String? {
        val keywords = extractKeywordsFromFavorites(favoriteArticles)
        return keywords.firstOrNull() // 가장 빈도가 높은 키워드 반환
    }
    
    /**
     * 추천 이유 생성
     */
    fun generateRecommendationReason(
        favoriteArticles: List<NewsArticle>,
        keyword: String
    ): String {
        val count = favoriteArticles.count { it.title.contains(keyword) }
        return "즐겨찾기한 기사 중 '$keyword' 관련 뉴스가 ${count}개 있어 추천드립니다"
    }
}
