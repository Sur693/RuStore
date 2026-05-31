package com.rustore.app.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.model.CategoryInfo
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<CategoryInfo>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = appRepository.getCategories()
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Неизвестная ошибка") }
            )
        }
    }

    fun refresh() {
        loadCategories()
    }

    class Factory(
        private val appRepository: AppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
                return CategoriesViewModel(appRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
