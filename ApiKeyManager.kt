package com.example.newsaisummary.data.config

/**
 * API 키 관리자
 * BuildConfig 문제 해결을 위한 대안
 * 
 * ⚠️ 사용 방법:
 * 1. 아래 API_KEY 변수에 실제 OpenAI API 키를 입력하세요
 * 2. 이 파일을 .gitignore에 추가하여 Git에 커밋되지 않도록 하세요
 */
object ApiKeyManager {
    
    /**
     * OpenAI API 키
     * 
     * ⚠️ 여기에 실제 OpenAI API 키를 입력하세요!
     * 
     * OpenAI API 키 발급 방법:
     * 1. https://platform.openai.com 접속
     * 2. 로그인 후 API keys 메뉴로 이동
     * 3. Create new secret key 클릭
     * 4. 생성된 키를 복사하여 아래에 붙여넣기
     * 
     * 예시: "sk-proj-abc123def456..."
     */
    private const val API_KEY = "여기에_실제_OpenAI_API_키_입력"
    
    /**
     * OpenAI API 키 가져오기
     */
    fun getOpenAiApiKey(): String {
        return API_KEY
    }
    
    /**
     * API 키가 올바르게 설정되었는지 확인
     * @return true if API key is properly set, false otherwise
     */
    fun isApiKeySet(): Boolean {
        val key = getOpenAiApiKey()
        return key.isNotEmpty() && 
               key != "여기에_실제_OpenAI_API_키_입력" &&
               key.startsWith("sk-")
    }
    
    /**
     * API 키 상태 메시지
     */
    fun getStatusMessage(): String {
        return if (isApiKeySet()) {
            "API 키가 정상적으로 설정되었습니다."
        } else {
            """
            API 키가 설정되지 않았습니다.
            
            ApiKeyManager.kt 파일을 열고
            API_KEY 변수에 실제 OpenAI API 키를 입력해주세요.
            
            키 발급: https://platform.openai.com
            """.trimIndent()
        }
    }
}
