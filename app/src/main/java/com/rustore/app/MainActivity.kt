package com.rustore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.rustore.app.ui.navigation.AppNavHost
import com.rustore.app.ui.theme.RuStoreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as RuStoreApplication).appContainer
        setContent {
            RuStoreTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(appContainer = appContainer)
                }
            }
        }
    }
}
