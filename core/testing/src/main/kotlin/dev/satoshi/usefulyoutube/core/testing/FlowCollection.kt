package dev.satoshi.usefulyoutube.core.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** Keeps [flow] collected for the rest of the test, so `stateIn(WhileSubscribed)` flows stay active. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.collectInBackground(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
}
