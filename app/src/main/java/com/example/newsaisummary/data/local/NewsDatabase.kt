package com.example.newsaisummary.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.newsaisummary.data.model.NewsArticle

/**
 * Room 데이터베이스 클래스
 * 뉴스 기사를 로컬에 저장하기 위한 데이터베이스
 */
@Database(
    entities = [NewsArticle::class],
    version = 2,  // isTest 필드 추가로 버전 업그레이드
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {

    /**
     * NewsDao 인스턴스 제공
     */
    abstract fun newsDao(): NewsDao

    companion object {
        @Volatile
        private var INSTANCE: NewsDatabase? = null

        /**
         * 싱글톤 데이터베이스 인스턴스 가져오기
         * 스레드 안전하게 구현
         */
        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "news_database"
                )
                    .fallbackToDestructiveMigration() // 마이그레이션 실패 시 데이터 삭제 후 재생성
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}