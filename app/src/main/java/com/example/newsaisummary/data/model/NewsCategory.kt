package com.example.newsaisummary.data.model

/**
 * 뉴스 카테고리 Enum (연예/스포츠 제거)
 */
enum class NewsCategory(
    val displayName: String,
    val naverSectionId: String?,
    val color: Long
) {
    ALL("전체", null, 0xFF6200EE),
    POLITICS("정치", "100", 0xFF1976D2),
    ECONOMY("경제", "101", 0xFF388E3C),
    SOCIETY("사회", "102", 0xFFD32F2F),
    LIFE("생활", "103", 0xFFFFA000),
    WORLD("세계", "104", 0xFF0097A7),
    IT("IT/과학", "105", 0xFF7B1FA2);
    
    companion object {
        fun fromSectionId(sectionId: String): NewsCategory {
            return values().find { it.naverSectionId == sectionId } ?: ALL
        }
    }
}
