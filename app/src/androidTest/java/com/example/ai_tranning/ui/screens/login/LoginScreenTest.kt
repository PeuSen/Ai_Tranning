package com.example.ai_tranning.ui.screens.login

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ai_tranning.data.local.AppDatabase
import com.example.ai_tranning.data.repository.UserRepository
import com.example.ai_tranning.ui.theme.Ai_TranningTheme
import com.example.ai_tranning.utils.SessionManager
import com.example.ai_tranning.viewmodel.LoginViewModel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Compose UI tests for [LoginScreen].
 *
 * The screen is driven by a real [LoginViewModel] wired to an in-memory Room database and a
 * throwaway DataStore — no mocking framework is needed on the instrumented classpath. These verify
 * rendering, client-side validation, and the field-binding wired through `StateFlow`.
 *
 * Runs as an instrumented test (`connectedAndroidTest`) since Compose UI testing needs a device or
 * emulator.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefsFile: File
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsFile = File(context.cacheDir, "login_ui_test_${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        viewModel = LoginViewModel(UserRepository(db.userDao()), SessionManager(dataStore))
    }

    @After
    fun tearDown() {
        db.close()
        prefsFile.delete()
    }

    private fun setContent(onLoginSuccess: () -> Unit = {}) {
        composeTestRule.setContent {
            Ai_TranningTheme {
                LoginScreen(
                    onLoginSuccess = onLoginSuccess,
                    onNavigateToRegister = {},
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun rendersFormFields() {
        setContent()

        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account? Register").assertIsDisplayed()
    }

    @Test
    fun submittingEmptyFormShowsValidationError() {
        var navigated = false
        setContent(onLoginSuccess = { navigated = true })

        // The header and the button both read "Login"; the button is the last match.
        composeTestRule.onAllNodesWithText("Login").onLast().performClick()

        composeTestRule.onNodeWithText("Please fill in all fields").assertIsDisplayed()
        assertFalse(navigated)
    }

    @Test
    fun typingUpdatesTheUsernameField() {
        setContent()

        composeTestRule.onNodeWithText("Username").performTextInput("alice")

        composeTestRule.onNodeWithText("alice").assertIsDisplayed()
    }
}