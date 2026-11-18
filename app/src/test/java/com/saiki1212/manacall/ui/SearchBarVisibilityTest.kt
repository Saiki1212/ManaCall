package com.saiki1212.manacall.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchBarVisibilityTest {
    @Test
    fun searchReturnsOnlyAfterFiveUninterruptedSeconds() = runTest {
        val scrolling = MutableStateFlow(false to false)
        var hidden = false
        backgroundScope.launch { scrolling.observeSearchBar { hidden = it } }
        runCurrent()

        scrolling.value = true to true
        runCurrent()
        assertTrue(hidden)

        scrolling.value = false to true
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(hidden)

        scrolling.value = true to false
        runCurrent()
        advanceTimeBy(6_000)
        runCurrent()
        assertTrue(hidden)

        scrolling.value = false to false
        runCurrent()
        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(hidden)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(hidden)
    }
}
