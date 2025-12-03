# 📰 AI 뉴스 요약 앱 (News AI Summary)

> 네이버 뉴스를 자동 스크랩하고 OpenAI GPT API를 활용하여 뉴스를 간결하게 요약해주는 안드로이드 애플리케이션

---

## 📌 개요

**학번**: 2021145046  
**이름**: 원재혁  
**프로젝트명**: AI 뉴스 요약 앱  
**개발 기간**: 2025년 하계 방학 프로젝트  

본 앱은 바쁜 현대인을 위해 최신 뉴스를 자동으로 수집하고, AI 기술을 활용하여 핵심 내용만 빠르게 파악할 수 있도록 도와주는 애플리케이션입니다.

---

## ✨ 주요 기능

### 🔍 뉴스 스크랩 & 검색
- **카테고리별 최신 뉴스**: 정치, 경제, 사회, 생활/문화, IT/과학 등 카테고리별 뉴스 자동 수집
- **키워드 검색**: 관심 있는 주제의 뉴스를 검색어로 찾기
- **네이버 뉴스 API + 웹 크롤링**: API 실패 시 자동으로 크롤링 방식 전환

### 🤖 AI 요약
- **OpenAI GPT-3.5-Turbo** 활용
- 긴 기사를 3-5문장으로 압축
- 핵심 내용과 중요 수치 자동 추출

### 💾 저장 & 관리
- **Room Database** 기반 로컬 저장
- 요약된 뉴스 자동 저장
- 즐겨찾기 기능으로 중요 뉴스 북마크

### 🌟 즐겨찾기 추천
- 즐겨찾기한 뉴스 분석
- AI 키워드 추출 알고리즘
- 관심사 기반 맞춤 뉴스 추천

### 📱 직관적인 UI/UX
- **Jetpack Compose** 기반 최신 UI
- Material Design 3 적용
- 다크 모드 지원
- 부드러운 애니메이션

---

## 🛠️ 기술 스택

### Frontend
- **Kotlin** - 안드로이드 공식 언어
- **Jetpack Compose** - 선언형 UI 프레임워크
- **Material Design 3** - 최신 디자인 가이드라인

### Backend & API
- **Retrofit 2** - HTTP 클라이언트
- **OkHttp** - 네트워크 통신
- **Jsoup** - HTML 파싱 및 웹 크롤링
- **Naver Search API** - 뉴스 검색
- **OpenAI API** - GPT-3.5-Turbo 요약

### Database
- **Room** - 로컬 데이터베이스
- **Kotlin Coroutines** - 비동기 처리
- **Flow** - 반응형 데이터 스트림

### Architecture
- **MVVM Pattern** - Model-View-ViewModel
- **Repository Pattern** - 데이터 레이어 추상화
- **Clean Architecture** - 계층 분리

---

## 📂 프로젝트 구조

```
NewsAiSummary/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/newsaisummary/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/           # API 인터페이스
│   │   │   │   │   ├── local/         # Room Database
│   │   │   │   │   ├── model/         # 데이터 모델
│   │   │   │   │   ├── repository/    # Repository 패턴
│   │   │   │   │   └── scraper/       # 웹 스크래핑
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/       # Compose 화면
│   │   │   │   │   ├── theme/         # 테마 설정
│   │   │   │   │   └── viewmodel/     # ViewModel
│   │   │   │   ├── utils/             # 유틸리티
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/                    # 리소스 파일
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 🚀 설치 및 실행

### 1. 사전 요구사항
- Android Studio (Hedgehog 이상)
- JDK 17 이상
- Android SDK (API 24 이상)
- OpenAI API 키
- 네이버 개발자 API 키 (선택)

### 2. API 키 설정

#### `local.properties` 파일에 추가:
```properties
sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk

# OpenAI API 키 (필수)
OPENAI_API_KEY=sk-proj-your-api-key-here

# 네이버 검색 API 키 (선택)
NAVER_CLIENT_ID=your_client_id_here
NAVER_CLIENT_SECRET=your_client_secret_here
```

### 3. 프로젝트 빌드
```bash
# 1. 프로젝트 클론
git clone https://github.com/yourusername/NewsAiSummary.git
cd NewsAiSummary

# 2. Android Studio에서 프로젝트 열기
# File → Open → NewsAiSummary 폴더 선택

# 3. Gradle Sync
# 자동으로 실행되거나 File → Sync Project with Gradle Files

# 4. 앱 실행
# Run 버튼 클릭 또는 Shift+F10
```

---

## 📖 사용 방법

### 1. 최신 뉴스 보기
1. 상단의 카테고리 선택 (전체/정치/경제/사회/생활/IT)
2. "최신 뉴스" 버튼 클릭
3. 원하는 뉴스 선택 후 "요약하기" 버튼 클릭

### 2. 뉴스 검색
1. 검색창에 키워드 입력 (예: "AI", "경제")
2. "검색" 버튼 클릭
3. 검색 결과에서 원하는 뉴스 선택

### 3. 저장된 요약 보기
1. "저장된 요약" 탭 선택
2. 저장된 뉴스 목록 확인
3. 다이아몬드 하트 아이콘으로 즐겨찾기 추가

### 4. 즐겨찾기 & 추천
1. "즐겨찾기" 탭 선택
2. 즐겨찾기한 뉴스 목록 확인
3. "관련 뉴스 찾기" 버튼으로 맞춤 추천 받기

---

## 🎨 주요 화면

### 메인 화면 (최신 뉴스)
- 카테고리별 필터링
- 검색 기능
- 뉴스 카드 리스트

### 저장된 요약
- AI 요약문 표시
- 전체 기사 링크
- 즐겨찾기 토글
- 삭제 기능

### 즐겨찾기
- 즐겨찾기 목록
- 관련 뉴스 추천
- 카테고리 필터

---

## 🔧 주요 알고리즘

### 뉴스 스크래핑
```kotlin
// 1. 네이버 API 시도
val apiResults = naverNewsApi.searchNews(keyword)

// 2. API 실패 시 크롤링 대체
if (apiResults.isEmpty()) {
    val scrapedNews = jsoupScraper.scrapeNews(keyword)
}

// 3. 불필요한 항목 필터링
val filtered = news.filter { 
    !it.title.contains("언론사 선정") 
}
```

### AI 요약
```kotlin
// GPT-3.5-Turbo 활용
val summary = openAiApi.createChatCompletion(
    model = "gpt-3.5-turbo",
    messages = [
        systemMessage("간결하게 3-5문장으로 요약"),
        userMessage(articleContent)
    ]
)
```

### 추천 알고리즘
```kotlin
// 1. 즐겨찾기 제목 분석
val titles = favorites.map { it.title }

// 2. 키워드 추출 (빈도 기반)
val keywords = extractKeywords(titles)
    .filter { !stopWords.contains(it) }
    .filter { !mediaNames.contains(it) }

// 3. 가장 빈도 높은 키워드로 검색
val recommendations = searchNews(keywords.first())
```

---

## 📊 성능 최적화

### 네트워크
- Retrofit + OkHttp 활용
- 연결 타임아웃: 15초
- HTTP 로깅 인터셉터

### 데이터베이스
- Room 인덱싱
- Flow 기반 반응형 업데이트
- Coroutine 비동기 처리

### UI
- LazyColumn 가상화 리스트
- remember/mutableState 최적화
- 불필요한 Recomposition 최소화

---

## 🐛 알려진 이슈 및 해결 방법

### Issue 1: 네이버 API 401 오류
**원인**: API 키 미설정  
**해결**: `local.properties`에 올바른 API 키 입력

### Issue 2: 웹 크롤링 실패
**원인**: 네이버 HTML 구조 변경  
**해결**: 여러 CSS 선택자 사용 + fallback 로직

### Issue 3: AI 요약 느림
**원인**: GPT API 응답 지연  
**해결**: 로딩 인디케이터 표시 + Coroutine 비동기 처리

---

## 🔐 보안

### API 키 관리
- `local.properties` 사용 (Git 제외)
- BuildConfig를 통한 안전한 접근
- 키 노출 방지

### 데이터 보호
- Room Database 암호화
- HTTPS 통신
- 민감 정보 제외
- 
---


### 사용된 오픈소스 라이브러리
- Retrofit (Apache 2.0)
- OkHttp (Apache 2.0)
- Jsoup (MIT)
- Room (Apache 2.0)
- Jetpack Compose (Apache 2.0)

---
