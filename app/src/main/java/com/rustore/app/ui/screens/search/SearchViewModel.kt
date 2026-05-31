package com.rustore.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<AppItem>>>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _popularAppsState = MutableStateFlow<UiState<List<AppItem>>>(UiState.Loading)
    val popularAppsState = _popularAppsState.asStateFlow()

    init {
        loadPopularApps()
        viewModelScope.launch {
            _query
                .debounce(300L) // не делаем запрос на каждую букву
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.value = UiState.Success(emptyList())
                    } else {
                        performSearch(query)
                    }
                }
        }
    }

    private fun loadPopularApps() {
        viewModelScope.launch {
            _popularAppsState.value = UiState.Loading
            val result = appRepository.getPopularApps()
            _popularAppsState.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Неизвестная ошибка") }
            )
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = UiState.Loading
        val result = appRepository.searchApps(query)
        _uiState.value = result.fold(
            onSuccess = { UiState.Success(it) },
            onFailure = { UiState.Error(it.message ?: "Неизвестная ошибка") }
        )
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun retrySearch() {
        val currentQuery = _query.value
        if (currentQuery.isNotBlank()) {
            viewModelScope.launch { performSearch(currentQuery) }
        }
    }

    fun retryPopular() {
        loadPopularApps()
    }

    class Factory(
        private val appRepository: AppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                return SearchViewModel(appRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
