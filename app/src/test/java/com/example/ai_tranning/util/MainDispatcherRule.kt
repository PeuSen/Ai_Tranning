package com.example.ai_tranning.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that swaps the [Dispatchers.Main] dispatcher for a [TestDispatcher] for the duration
 * of a test, then restores the original dispatcher afterwards.
 *
 * This is required for any test that exercises a `ViewModel`, because `viewModelScope` is hard-wired
 * to [Dispatchers.Main] which is unavailable on the local JVM (there is no Android main looper).
 *
 * An [UnconfinedTestDispatcher] is used by default so that coroutines launched inside the code under
 * test run eagerly and synchronously — assertions can be made immediately after the call that
 * triggers the coroutine, without having to manually advance a scheduler.
 *
 * Usage:
 * ```
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 *
 * @param testDispatcher the dispatcher installed as `Main`. Override with a
 *   `StandardTestDispatcher` when a test needs explicit control over virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}