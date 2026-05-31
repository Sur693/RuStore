package com.rustore.app.ui.screens.appdetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rustore.app.AppContainer
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.model.DownloadStatus
import com.rustore.app.data.model.DownloadTask
import com.rustore.app.service.DownloadService
import com.rustore.app.service.PackageInstallerHelper
import com.rustore.app.ui.components.AppIcon
import com.rustore.app.ui.components.ErrorContent
import com.rustore.app.ui.components.LoadingContent
import com.rustore.app.ui.components.ScreenshotPlaceholder
import com.rustore.app.ui.state.UiState
import com.rustore.app.ui.theme.Primary
import kotlinx.coroutines.launch

// Карточка приложения: иконка, разработчик, скриншоты, описание, кнопка загрузки
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appContainer: AppContainer,
    appId: String,
    onBack: () -> Unit,
    onScreenshotClick: (Int) -> Unit
) {
    // key = appId чтобы ViewModel пересоздавалась при переходе на другое приложение
    val viewModel: AppDetailViewModel = viewModel(
        key = appId,
        factory = AppDetailViewModel.Factory(
            appContainer.appRepository,
            appContainer.downloadRepository,
            appId
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val downloadTask by viewModel.downloadTask.collectAsState()

    val context = LocalContext.current

    // После перезапуска задача теряется из памяти — проверяем реальную установку через PackageManager
    val effectiveTask = remember(downloadTask, uiState) {
        if (downloadTask == null && uiState is UiState.Success) {
            val pkg = (uiState as UiState.Success<AppItem>).data.packageName
            if (pkg.isNotEmpty()) {
                try {
                    context.packageManager.getPackageInfo(pkg, 0)
                    DownloadTask(
                        taskId = "installed_$appId",
                        appId = appId,
                        appName = (uiState as UiState.Success<AppItem>).data.name,
                        status = DownloadStatus.INSTALLED,
                        packageName = pkg
                    )
                } catch (e: Exception) { null }
            } else null
        } else downloadTask
    }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // Запрос разрешения на уведомления
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* разрешение запрошено */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is UiState.Success) {
                        Text(
                            text = (uiState as UiState.Success<AppItem>).data.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                    } else {
                        Text("Приложение")
                    }
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
                is UiState.Loading -> LoadingContent()
                is UiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadApp() }
                )
                is UiState.Success -> {
                    AppDetailContent(
                        app = state.data,
                        downloadTask = effectiveTask,
                        onScreenshotClick = onScreenshotClick,
                        onInstallClick = {
                            // Запрашиваем разрешение на уведомления перед запуском сервиса
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            // Запускаем загрузку через Foreground Service
                            viewModel.startDownload(state.data.name) { taskId ->
                                DownloadService.startDownload(context, taskId, appId, state.data.name)
                            }
                        },
                        onCancelClick = {
                            viewModel.cancelDownload { taskId ->
                                DownloadService.cancelDownload(context, taskId)
                            }
                        },
                        onInstallApkClick = {
                            val apkPath = effectiveTask?.apkPath
                            val pkg = apkPath?.let {
                                @Suppress("DEPRECATION")
                                context.packageManager.getPackageArchiveInfo(it, 0)?.packageName
                            }
                            viewModel.installApk(pkg) { taskId, path ->
                                PackageInstallerHelper.installApk(context, path, state.data.name, taskId)
                            }
                        },
                        onUninstallClick = {
                            val pkg = effectiveTask?.packageName?.takeIf { it.isNotEmpty() }
                                ?: state.data.packageName.takeIf { it.isNotEmpty() }
                            pkg?.let { PackageInstallerHelper.uninstallPackage(context, it) }
                        },
                        onOpenClick = {
                            val pkg = effectiveTask?.packageName?.takeIf { it.isNotEmpty() }
                                ?: state.data.packageName.takeIf { it.isNotEmpty() }
                            pkg?.let { packageName ->
                                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                intent?.let { context.startActivity(it) }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDetailContent(
    app: AppItem,
    downloadTask: DownloadTask?,
    onScreenshotClick: (Int) -> Unit,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onInstallApkClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Шапка: иконка, название, разработчик, статистика, кнопка установки
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon(
                    iconUrl = app.iconUrl,
                    size = 96.dp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.developer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Статистика: рейтинг, загрузки, возраст
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppStat(label = "Рейтинг", value = String.format("%.1f", app.rating))
                VerticalStatDivider()
                AppStat(label = "Загрузки", value = app.downloadCount)
                VerticalStatDivider()
                AppStat(label = "Возраст", value = app.ageRating.label)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка установки меняет вид в зависимости от состояния загрузки
            InstallButton(
                downloadTask = downloadTask,
                hasPackageName = app.packageName.isNotEmpty() || downloadTask?.packageName?.isNotEmpty() == true,
                onInstallClick = onInstallClick,
                onCancelClick = onCancelClick,
                onInstallApkClick = onInstallApkClick,
                onUninstallClick = onUninstallClick,
                onOpenClick = onOpenClick
            )
        }

        HorizontalDivider()

        // Скриншоты — горизонтальный список с переходом на полноэкранный просмотр
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            itemsIndexed(app.screenshotUrls) { index, url ->
                ScreenshotPlaceholder(
                    url = url,
                    width = 120.dp,
                    height = 213.dp,
                    onClick = { onScreenshotClick(index) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        HorizontalDivider()

        // Описание приложения
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Описание",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.fullDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )
        }

        HorizontalDivider()

        // Информация о приложении
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Информация",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppInfoRow(label = "Разработчик", value = app.developer)
            AppInfoRow(label = "Категория", value = app.category.displayName)
            AppInfoRow(label = "Возрастной рейтинг", value = app.ageRating.label)
            AppInfoRow(label = "Загрузки", value = app.downloadCount)
            AppInfoRow(label = "Рейтинг", value = "${String.format("%.1f", app.rating)} ★")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// Кнопка установки — 5 состояний: обычная / очередь+загрузка / завершено / устанавливается / установлено
@Composable
private fun InstallButton(
    downloadTask: DownloadTask?,
    hasPackageName: Boolean,
    onInstallClick: () -> Unit,
    onCancelClick: () -> Unit,
    onInstallApkClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    when {
        downloadTask == null || downloadTask.status == DownloadStatus.CANCELLED ||
        downloadTask.status == DownloadStatus.ERROR -> {
            // Обычное состояние: кнопка «Установить»
            Button(
                onClick = onInstallClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (downloadTask?.status == DownloadStatus.ERROR) "Повторить загрузку"
                           else "Установить",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        downloadTask.status == DownloadStatus.QUEUED ||
        downloadTask.status == DownloadStatus.DOWNLOADING -> {
            // Идёт загрузка: прогресс-бар
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (downloadTask.status == DownloadStatus.QUEUED) "В очереди..."
                           else "Загрузка ${downloadTask.progress}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadTask.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Primary
                )
            }
        }

        downloadTask.status == DownloadStatus.COMPLETED -> {
            // Загрузка завершена — передаём APK системному PackageInstaller
            Button(
                onClick = onInstallApkClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Установить APK", style = MaterialTheme.typography.titleSmall)
            }
        }

        downloadTask.status == DownloadStatus.INSTALLING -> {
            // PackageInstaller обрабатывает APK — показываем спиннер
            Button(
                onClick = { },
                enabled = false,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Устанавливается...", style = MaterialTheme.typography.titleSmall)
            }
        }

        downloadTask.status == DownloadStatus.INSTALLED -> {
            // Установлено: кнопка удаления (если есть packageName)
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onOpenClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Открыть", style = MaterialTheme.typography.titleSmall)
                }
                if (hasPackageName) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onUninstallClick,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }

        else -> { }
    }
}

@Composable
private fun AppStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Вертикальный разделитель между статистическими показателями
@Composable
private fun VerticalStatDivider() {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

// Строка информации
@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.55f)
        )
    }
}
