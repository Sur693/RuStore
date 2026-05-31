package com.rustore.app.ui.screens.appdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.download.DownloadRepository
import com.rustore.app.data.model.AppItem
import com.rustore.app.data.model.DownloadStatus
import com.rustore.app.data.model.DownloadTask
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel карточки приложения — загружает данные и управляет загрузкой/установкой APK
class AppDetailViewModel(
    private val appRepository: AppRepository,
    private val downloadRepository: DownloadRepository,
    private val appId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AppItem>>(UiState.Loading)
    val uiState: StateFlow<UiState<AppItem>> = _uiState.asStateFlow()

    // Текущее состояние задачи загрузки для этого приложения
    private val _downloadTask = MutableStateFlow<DownloadTask?>(null)
    val downloadTask: StateFlow<DownloadTask?> = _downloadTask.asStateFlow()

    // Текущий taskId — нужен для отмены и установки
    private var currentTaskId: String? = null

    init {
        loadApp()
        // Наблюдаем за всеми задачами и фильтруем по appId
        viewModelScope.launch {
            downloadRepository.activeTasks.collect { tasks ->
                val task = tasks.values.firstOrNull { it.appId == appId }
                _downloadTask.value = task
                if (task != null) currentTaskId = task.taskId
            }
        }
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

    // Запускает загрузку APK через Foreground Service
    fun startDownload(appName: String, onStartService: (taskId: String) -> Unit) {
        viewModelScope.launch {
            val taskId = downloadRepository.startDownload(appId, appName)
            currentTaskId = taskId
            onStartService(taskId)
        }
    }

    // Отменяет текущую загрузку
    fun cancelDownload(onCancelService: (taskId: String) -> Unit) {
        val taskId = currentTaskId ?: _downloadTask.value?.taskId ?: return
        viewModelScope.launch {
            downloadRepository.cancelDownload(taskId)
            onCancelService(taskId)
        }
    }

    // Передаёт APK системному PackageInstaller после завершения загрузки
    fun installApk(packageName: String?, onInstall: (taskId: String, apkPath: String) -> Unit) {
        val task = _downloadTask.value ?: return
        if (task.status != DownloadStatus.COMPLETED) return
        val apkPath = task.apkPath ?: return
        downloadRepository.updateStatus(task.taskId, DownloadStatus.INSTALLING, packageName = packageName)
        onInstall(task.taskId, apkPath)
    }

    class Factory(
        private val appRepository: AppRepository,
        private val downloadRepository: DownloadRepository,
        private val appId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppDetailViewModel::class.java)) {
                return AppDetailViewModel(appRepository, downloadRepository, appId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
