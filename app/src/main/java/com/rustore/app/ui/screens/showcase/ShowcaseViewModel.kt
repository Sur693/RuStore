package com.rustore.app.ui.screens.showcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.model.AppCategory
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShowcaseViewModel(
    private val appRepository: AppRepository,
    initialCategory: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<AppItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategory?>(
        initialCategory?.let { name ->
            AppCategory.entries.find { it.name == name } // строка из навигации → enum
        }
    )
    val selectedCategory = _selectedCategory.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = appRepository.getApps(_selectedCategory.value)
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Неизвестная ошибка") }
            )
        }
    }

    fun selectCategory(category: AppCategory?) {
        if (_selectedCategory.value != category) { // меняем только если отличается
            _selectedCategory.value = category
            loadApps()
        }
    }

    fun refresh() {
        loadApps()
    }

    class Factory(
        private val appRepository: AppRepository,
        private val initialCategory: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShowcaseViewModel::class.java)) {
                return ShowcaseViewModel(appRepository, initialCategory) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
