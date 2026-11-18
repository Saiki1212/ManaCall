package com.saiki1212.manacall.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

internal suspend fun Flow<Pair<Boolean, Boolean>>.observeSearchBar(setHidden: (Boolean) -> Unit) {
    collectLatest { (scrolling, forward) ->
        if (scrolling) {
            if (forward) setHidden(true)
        } else {
            delay(5_000L)
            setHidden(false)
        }
    }
}
