package com.kutluoglu.prayer_feature.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

@ExperimentalCoroutinesApi
class MainCoroutineRule(
    private val testScheduler: TestCoroutineScheduler = TestCoroutineScheduler()
) : AfterEachCallback, BeforeEachCallback {

    private lateinit var testDispatcher: TestDispatcher

    val dispatcher: TestDispatcher
        get() = testDispatcher

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }

    override fun beforeEach(context: ExtensionContext) {
        testDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)
    }
}