package com.rustore.app

import android.content.Context
import com.rustore.app.data.api.ApiAppRepository
import com.rustore.app.data.api.ApiService
import com.rustore.app.data.download.DownloadRepository
import com.rustore.app.data.download.DownloadRepositoryImpl
import com.rustore.app.data.preferences.OnboardingPreferences
import com.rustore.app.data.repository.AppRepository
import com.rustore.app.network.NetworkConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Ручной DI-контейнер: создаёт и хранит все зависимости приложения
class AppContainer(context: Context) {

    // OkHttpClient с увеличенным таймаутом для SSE-потока (readTimeout = долгое соединение)
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(NetworkConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)

    // Данные приходят с API-сервера вместо мок-репозитория
    val appRepository: AppRepository = ApiAppRepository(apiService)

    val onboardingPreferences: OnboardingPreferences = OnboardingPreferences(context)

    // Синглтон репозитория загрузок — StateFlow наблюдается из ViewModel и Service
    val downloadRepository: DownloadRepository = DownloadRepositoryImpl(
        context.getSharedPreferences("download_state", Context.MODE_PRIVATE)
    )
}
