package com.example.ai_tranning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.ai_tranning.ui.navigation.NavGraph
import com.example.ai_tranning.ui.navigation.Routes
import com.example.ai_tranning.ui.theme.Ai_TranningTheme
import com.example.ai_tranning.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host for the entire Compose UI.
 *
 * As an `@AndroidEntryPoint` it can have dependencies injected by Hilt — here the [SessionManager],
 * which it reads on launch to choose the start destination: the dashboard if a session exists,
 * otherwise the login screen. The first emission of `loggedInUserId` gates rendering so navigation
 * starts from the correct place without a visible flicker.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Injected session store; used to decide the navigation start destination. */
    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ai_TranningTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loggedInUserId by sessionManager.loggedInUserId.collectAsState(initial = null)
                    var isReady by remember { mutableStateOf(false) }

                    LaunchedEffect(loggedInUserId) {
                        // Wait for first emission to determine start destination
                        isReady = true
                    }

                    if (isReady) {
                        val startDestination = if (loggedInUserId != null) {
                            Routes.DASHBOARD
                        } else {
                            Routes.LOGIN
                        }
                        val navController = rememberNavController()
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    }
                }
            }
        }
    }
}