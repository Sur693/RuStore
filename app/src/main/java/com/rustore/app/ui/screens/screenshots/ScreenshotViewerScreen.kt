package com.rustore.app.ui.screens.screenshots

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rustore.app.AppContainer
import com.rustore.app.data.model.AppItem
import com.rustore.app.ui.components.ErrorContent
import com.rustore.app.ui.components.FullscreenScreenshotPlaceholder
import com.rustore.app.ui.components.LoadingContent
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.launch

// Полноэкранный просмотр скриншотов с HorizontalPager и индикатором страниц
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotViewerScreen(
    appContainer: AppContainer,
    appId: String,
    initialIndex: Int,
    onBack: () -> Unit
) {
    // Отдельный key чтобы ViewModel не конфликтовала с AppDetailViewModel
    val viewModel: ScreenshotViewerViewModel = viewModel(
        key = "screenshots_$appId",
        factory = ScreenshotViewerViewModel.Factory(appContainer.appRepository, appId)
    )
    val uiState by viewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is UiState.Success) {
                        val app = (uiState as UiState.Success<AppItem>).data
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
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
            when (val state = uiState) {
                is UiState.Loading -> LoadingContent(
                    modifier = Modifier.background(Color.Black)
                )
                is UiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadApp() }
                )
                is UiState.Success -> {
                    val app = state.data
                    val useUrls = app.screenshotUrls.isNotEmpty()
                    val count = app.screenshotUrls.size

                    if (count == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Нет скриншотов",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        val pagerState = rememberPagerState(
                            initialPage = initialIndex.coerceIn(0, count - 1),
                            pageCount = { count }
                        )
                        Box(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                FullscreenScreenshotPlaceholder(
                                    url = if (useUrls) app.screenshotUrls[page] else null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "${pagerState.currentPage + 1} / $count",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .background(
                                        color = Color.Black.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
