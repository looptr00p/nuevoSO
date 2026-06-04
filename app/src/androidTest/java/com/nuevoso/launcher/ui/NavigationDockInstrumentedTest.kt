package com.nuevoso.launcher.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.nuevoso.launcher.MainActivity
import org.junit.Rule
import org.junit.Test

class NavigationDockInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dockNavigatesAcrossPrimarySurfaces() {
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()

        composeRule.onNodeWithTag("dock_apps").performClick()
        composeRule.onNodeWithTag("screen_apps").assertIsDisplayed()
        composeRule.onAllNodesWithTag("dock_conversation").assertCountEquals(0)

        composeRule.onNodeWithTag("dock_settings").performClick()
        composeRule.onNodeWithTag("screen_settings").assertIsDisplayed()

        composeRule.onNodeWithTag("dock_home").performClick()
        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun conversationKeepsMessageSubmittedFromHome() {
        val syntheticMessage = "mensaje sintetico de navegacion"

        composeRule.onNodeWithTag("screen_home").assertIsDisplayed()
        composeRule.onNodeWithTag("composer_input").performTextInput(syntheticMessage)
        composeRule.onNodeWithTag("composer_send").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("screen_conversation").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("screen_conversation").assertIsDisplayed()
        composeRule.onNodeWithText(syntheticMessage).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Voz").assertCountEquals(0)
    }
}
