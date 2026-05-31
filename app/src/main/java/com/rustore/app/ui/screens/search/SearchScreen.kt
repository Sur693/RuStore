package com.rustore.app.ui.screens.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rustore.app.AppContainer
import com.rustore.app.ui.components.AppListItem
import com.rustore.app.ui.components.ErrorContent
import com.rustore.app.ui.components.LoadingContent
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.launch

// Экран поиска: при пустом запросе — популярные приложения, иначе — результаты поиска
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    appContainer: AppContainer,
    onAppClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(appContainer.appRepository)
    )
    val query by viewModel.query.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val popularAppsState by viewModel.popularAppsState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Автоматически открываем клавиатуру при входе на экран
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Поиск в RuStore") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Очистить"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    if (query.isBlank()) viewModel.retryPopular() else viewModel.retrySearch()
                    isRefreshing = false
                }
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (query.isBlank()) {
                // Пустой запрос — показываем популярные
                PopularAppsSection(
                    popularAppsState = popularAppsState,
                    onAppClick = onAppClick,
                    onRetry = { viewModel.retryPopular() }
                )
            } else {
                // Есть запрос — показываем результаты поиска
                SearchResultsSection(
                    uiState = uiState,
                    query = query,
                    onAppClick = onAppClick,
                    onRetry = { viewModel.retrySearch() }
                )
            }
        }
    }
}

@Composable
private fun PopularAppsSection(
    popularAppsState: UiState<*>,
    onAppClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Популярные приложения",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        HorizontalDivider()
        when (val state = popularAppsState) {
            is UiState.Loading -> LoadingContent()
            is UiState.Error -> ErrorContent(
                message = state.message,
                onRetry = onRetry
            )
            is UiState.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                val apps = state.data as List<com.rustore.app.data.model.AppItem>
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(apps, key = { it.id }) { app ->
                        AppListItem(
                            app = app,
                            onClick = { onAppClick(app.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    uiState: UiState<*>,
    query: String,
    onAppClick: (String) -> Unit,
    onRetry: () -> Unit
) {
    when (val state = uiState) {
        is UiState.Loading -> LoadingContent()
        is UiState.Error -> ErrorContent(
            message = state.message,
            onRetry = onRetry
        )
        is UiState.Success<*> -> {
            @Suppress("UNCHECKED_CAST")
            val apps = state.data as List<com.rustore.app.data.model.AppItem>
            if (apps.isEmpty()) {
                // Ничего не найдено
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "По запросу «$query» ничего не найдено",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Попробуйте другой запрос или проверьте написание",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(apps, key = { it.id }) { app ->
                        AppListItem(
                            app = app,
                            onClick = { onAppClick(app.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 84.dp)
                        )
                    }
                }
            }
        }
    }
}
