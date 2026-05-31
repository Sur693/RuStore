package com.rustore.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.rustore.app.RuStoreApplication
import com.rustore.app.data.model.DownloadStatus

// Принимает результат установки APK от PackageInstaller
class InstallReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_INSTALL_RESULT = "com.rustore.app.INSTALL_RESULT"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_RESULT) return

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val repository = (context.applicationContext as RuStoreApplication).appContainer.downloadRepository

        // taskId начинается с "uninstall_" — это результат удаления
        if (taskId.startsWith("uninstall_")) {
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    // Android требует подтверждения — запускаем системный диалог удаления
                    val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                    confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    confirmIntent?.let { context.startActivity(it) }
                }
                PackageInstaller.STATUS_SUCCESS -> {
                    // Удаление подтверждено — убираем задачу из репозитория
                    val packageName = taskId.removePrefix("uninstall_")
                    val tasks = repository.activeTasks.value
                    val task = tasks.values.firstOrNull { it.packageName == packageName }
                    task?.let { repository.removeTask(it.taskId) }
                }
            }
            return
        }

        when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                repository.updateStatus(taskId, DownloadStatus.INSTALLED)
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirmIntent?.let { context.startActivity(it) }
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Ошибка установки"
                repository.updateStatus(taskId, DownloadStatus.ERROR, error = message)
            }
        }
    }
}
