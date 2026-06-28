package com.example.ai_tranning

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class and Hilt dependency-injection root.
 *
 * Annotated with [HiltAndroidApp] so Hilt generates the application-level component that all
 * `@AndroidEntryPoint` activities and `@HiltViewModel` view models are wired from. Registered as
 * `android:name` in the manifest. Holds no state of its own.
 */
@HiltAndroidApp
class AiTrainingApp : Application()