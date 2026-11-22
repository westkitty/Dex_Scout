package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ai.configureAppCheck
import com.example.ui.DexScoutApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before Compose UI or ViewModels.
        configureAppCheck(this, intent)

        // STEP 2: Enable edge-to-edge and launch DEX//SCOUT UI.
        enableEdgeToEdge()
        setContent {
            DexScoutApp()
        }
    }
}
