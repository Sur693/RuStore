package com.rustore.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rustore.app.data.preferences.OnboardingPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val onboardingPreferences: OnboardingPreferences
) : ViewModel() {

    // null — ещё не загружено, true/false — результат из DataStore
    private val _isOnboardingComplete = MutableStateFlow<Boolean?>(null)
    val isOnboardingComplete = _isOnboardingComplete.asStateFlow()

    init {
        viewModelScope.launch {
            val complete = onboardingPreferences.isOnboardingComplete.first() // читаем один раз
            _isOnboardingComplete.value = complete
        }
    }

    fun completeOnboarding(onComplete: () -> Unit) {
        viewModelScope.launch {
            onboardingPreferences.setOnboardingComplete()
            onComplete()
        }
    }

    class Factory(
        private val onboardingPreferences: OnboardingPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(onboardingPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
