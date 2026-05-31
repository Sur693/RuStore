package com.rustore.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File
import java.io.FileInputStream

// Хелпер для установки и удаления APK через PackageInstaller API
object PackageInstallerHelper {

    // Установка APK по пути к файлу
    // taskId нужен для обновления статуса через InstallReceiver
    fun installApk(context: Context, apkPath: String, appName: String, taskId: String) {
        val file = File(apkPath)
        if (!file.exists()) return

        val packageInstaller = context.packageManager.packageInstaller

        // Создаём сессию установки
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppLabel(appName)

        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        try {
            // Копируем байты APK в сессию установщика
            session.openWrite("package", 0, file.length()).use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            // PendingIntent для получения результата через InstallReceiver
            val resultIntent = Intent(context, InstallReceiver::class.java).apply {
                action = InstallReceiver.ACTION_INSTALL_RESULT
                putExtra(InstallReceiver.EXTRA_TASK_ID, taskId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                resultIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Передаём APK системному установщику
            session.commit(pendingIntent.intentSender)
        } catch (e: Exception) {
            session.abandon()
        } finally {
            session.close()
        }
    }

    // Удаление приложения через PackageInstaller — показывает системный диалог
    fun uninstallPackage(context: Context, packageName: String) {
        android.util.Log.d("RuStore", "uninstallPackage: $packageName")
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val resultIntent = Intent(context, InstallReceiver::class.java).apply {
                action = InstallReceiver.ACTION_INSTALL_RESULT
                putExtra(InstallReceiver.EXTRA_TASK_ID, "uninstall_$packageName")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, packageName.hashCode(), resultIntent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            packageInstaller.uninstall(packageName, pendingIntent.intentSender)
        } catch (e: Exception) {
            android.util.Log.e("RuStore", "uninstallPackage error: ${e.message}", e)
        }
    }
}
