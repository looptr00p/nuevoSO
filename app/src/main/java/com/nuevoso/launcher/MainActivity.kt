package com.nuevoso.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nuevoso.launcher.ui.chat.ChatScreen
import com.nuevoso.launcher.ui.drawer.AppDrawerScreen
import com.nuevoso.launcher.ui.settings.SettingsScreen
import com.nuevoso.launcher.ui.theme.NuevoSOTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuevoSOTheme {
                NuevoSOApp()
            }
        }
    }

    // Launchers must not finish on Back from the root screen
    override fun onBackPressed() {
        // consume: stay on home
    }
}

@Composable
private fun NuevoSOApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                onNavigateToDrawer = { navController.navigate("drawer") },
                onNavigateToSettings = { navController.navigate("settings") },
            )
        }
        composable("drawer") {
            AppDrawerScreen()
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}
