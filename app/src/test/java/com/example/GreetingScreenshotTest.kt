package com.example

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.ConstructionRepository
import com.example.ui.GoogleLoginScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent { MyApplicationTheme { Text("ConstructPro") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun login_screen_screenshot() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getDatabase(context)
    val repo = ConstructionRepository(db.constructionDao())
    val vm = MainViewModel(repo)
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        GoogleLoginScreen(viewModel = vm)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login_screen.png")
  }
}
