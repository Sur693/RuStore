package com.rustore.app.ui.screens.screenshots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ScreenshotViewerViewModel(
    private val appRepository: AppRepository,
    private val appId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AppItem>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadApp()
    }

    fun loadApp() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = appRepository.getApp(appId)
            _uiState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Неизвестная ошибка") }
            )
        }
    }

    fun refresh() {
        loadApp()
    }

    class Factory(
        private val appRepository: AppRepository,
        private val appId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScreenshotViewerViewModel::class.java)) {
                return ScreenshotViewerViewModel(appRepository, appId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
