package com.example.newsaisummary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.newsaisummary.data.repository.NewsRepository

/**
 * NewsViewModel 팩토리
 * ViewModel에 의존성을 주입하기 위한 팩토리 클래스
 */
class NewsViewModelFactory(
    private val repository: NewsRepository
) : ViewModelProvider.Factory {

    /**
     * ViewModel 인스턴스 생성
     * @param modelClass 생성할 ViewModel 클래스
     * @return ViewModel 인스턴스
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NewsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}