package com.rustore.app.ui.screens.showcase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rustore.app.AppContainer
import com.rustore.app.data.model.AppCategory
import com.rustore.app.ui.components.AppListItem
import com.rustore.app.ui.components.ErrorContent
import com.rustore.app.ui.components.LoadingContent
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.launch

// Главный экран — витрина приложений с фильтрацией по категориям
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowcaseScreen(
    appContainer: AppContainer,
    initialCategory: String?,
    onAppClick: (String) -> Unit,
    onCategoriesClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val viewModel: ShowcaseViewModel = viewModel(
        factory = ShowcaseViewModel.Factory(appContainer.appRepository, initialCategory)
    )
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RuStore",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Поиск"
                        )
                    }
                    IconButton(onClick = onCategoriesClick) {
                        Icon(
                            imageVector = Icons.Filled.Category,
                            contentDescription = "Категории"
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
                    viewModel.refresh()
                    isRefreshing = false
                }
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Чипы фильтрации по категориям
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )
                HorizontalDivider()
                when (val state = uiState) {
                    is UiState.Loading -> LoadingContent()
                    is UiState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadApps() }
                    )
                    is UiState.Success -> {
                        if (state.data.isEmpty()) {
                            EmptyShowcaseContent()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(
                                    items = state.data,
                                    key = { it.id }
                                ) { app ->
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
        }
    }
}

// Горизонтальная строка с чипами для выбора категории
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    selectedCategory: AppCategory?,
    onCategorySelected: (AppCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Все") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0078FF).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFF0078FF)
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        items(AppCategory.entries) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.displayName) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = category.color,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0078FF).copy(alpha = 0.15f),
                    selectedLabelColor = Color(0xFF0078FF),
                    selectedLeadingIconColor = category.color
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

// Заглушка когда в категории нет приложений
@Composable
private fun EmptyShowcaseContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            text = "Нет приложений в этой категории",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
