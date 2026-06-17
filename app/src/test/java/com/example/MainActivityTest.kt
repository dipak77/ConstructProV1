package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppLoginAndPinFlow() {
        val activity = composeTestRule.activity
        
        // Find MainViewModel from Lazy delegate
        val mainViewModel = activity.run {
            val field = this::class.java.getDeclaredFields().firstOrNull { 
                it.name.contains("viewModel") 
            }
            field?.isAccessible = true
            val lazyObj = field?.get(this) as? Lazy<*>
            lazyObj?.value as? com.example.ui.MainViewModel
        }

        println("=== DEBUG VM BEFORE CLICK ===")
        println("UserSession: ${mainViewModel?.userSession?.value}")
        println("IsBlocked: ${mainViewModel?.isUserBlocked?.value}")

        // 1. Initially, we should be on the Google Login Screen and see the app title
        composeTestRule.onNodeWithText("CONSTRUCTPRO").assertExists()
        composeTestRule.onNodeWithText("SECURE WORKSPACE PORTAL").assertExists()

        // 2. Directly trigger Google Sign In on the main thread to bypass any test animation looper stalls
        val user = com.example.ui.GoogleUser(
            displayName = "ConstructPro Demo",
            email = "demo.contractor@constructpro.net",
            photoUrl = null,
            isGuest = true
        )
        activity.runOnUiThread {
            mainViewModel?.handleGoogleSignIn(user, activity)
        }

        // Wait for composition to settle
        composeTestRule.waitForIdle()

        println("=== DEBUG VM AFTER CLICK ===")
        println("UserSession: ${mainViewModel?.userSession?.value}")
        println("IsBlocked: ${mainViewModel?.isUserBlocked?.value}")

        // Print final screen state 
        println("=== AFTER CLICK SCREEN TREE ===")
        printFullTree(composeTestRule.onRoot())

        // 3. Since guest bypass is disabled, we must land on the Security PIN Setup screen
        composeTestRule.onNodeWithText("Create PIN").assertExists()
        composeTestRule.onNodeWithText("Choose a 4-digit PIN").assertExists()
        composeTestRule.onNodeWithText("Skip for now").assertExists()

        // 4. Test clicking numbers on PIN screen
        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
    }

    private fun printFullTree(interaction: SemanticsNodeInteraction) {
        try {
            val node = interaction.fetchSemanticsNode()
            printNodeRecursive(node, 0)
        } catch (t: Throwable) {
            println("Failed to fetch node tree: ${t.message}")
        }
    }

    private fun printNodeRecursive(node: androidx.compose.ui.semantics.SemanticsNode, indent: Int) {
        val spaces = "  ".repeat(indent)
        val textList = node.config.getOrNull(SemanticsProperties.Text)
        val contentDesc = node.config.getOrNull(SemanticsProperties.ContentDescription)
        val testTag = node.config.getOrNull(SemanticsProperties.TestTag)
        
        println("${spaces}- Node [Tag: $testTag, Text: $textList, Desc: $contentDesc]")
        
        for (child in node.children) {
            printNodeRecursive(child, indent + 1)
        }
    }
}
