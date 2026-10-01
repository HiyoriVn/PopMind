package com.example.popmind.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.popmind.data.SessionRepository

class ProgressViewModelFactory(private val repository: SessionRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ProgressViewModel::class.java)) {
            throw IllegalArgumentException("ViewModel không được hỗ trợ: ${modelClass.name}")
        }
        return ProgressViewModel(repository) as T
    }
}
