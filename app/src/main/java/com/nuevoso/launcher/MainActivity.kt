package com.nuevoso.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nuevoso.launcher.ui.chat.ChatScreen
import com.nuevoso.launcher.ui.chat.ChatScreenMode
import com.nuevoso.launcher.ui.chat.ChatViewModel
import com.nuevoso.launcher.ui.chat.DockDestination
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

}

@Composable
private fun NuevoSOApp() {
    val navController = rememberNavController()
    val chatViewModel: ChatViewModel = viewModel()

    fun navigateTo(destination: DockDestination) {
        val route = when (destination) {
            DockDestination.Home -> ROUTE_HOME
            DockDestination.Apps -> ROUTE_DRAWER
            DockDestination.Settings -> ROUTE_SETTINGS
        }
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(ROUTE_HOME) {
                saveState = true
            }
        }
    }

    fun returnHome() {
        navController.navigate(ROUTE_HOME) {
            launchSingleTop = true
            popUpTo(ROUTE_HOME) {
                inclusive = false
            }
        }
    }

    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            BackHandler {
                // Launchers should stay on the root home surface when Back is pressed.
            }
            ChatScreen(
                mode = ChatScreenMode.Home,
                currentDestination = DockDestination.Home,
                onDockDestinationSelected = ::navigateTo,
                onConversationStarted = {
                    navController.navigate(ROUTE_CONVERSATION) {
                        launchSingleTop = true
                    }
                },
                vm = chatViewModel,
            )
        }
        composable(ROUTE_CONVERSATION) {
            BackHandler { returnHome() }
            ChatScreen(
                mode = ChatScreenMode.Conversation,
                currentDestination = DockDestination.Home,
                onDockDestinationSelected = ::navigateTo,
                onConversationStarted = {},
                vm = chatViewModel,
            )
        }
        composable(ROUTE_DRAWER) {
            BackHandler { returnHome() }
            AppDrawerScreen(onDockDestinationSelected = ::navigateTo)
        }
        composable(ROUTE_SETTINGS) {
            BackHandler { returnHome() }
            SettingsScreen(onDockDestinationSelected = ::navigateTo)
        }
    }
}

private const val ROUTE_HOME = "chat"
private const val ROUTE_CONVERSATION = "conversation"
private const val ROUTE_DRAWER = "drawer"
private const val ROUTE_SETTINGS = "settings"
