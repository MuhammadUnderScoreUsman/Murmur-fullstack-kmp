package com.example.chatappkross.auth.presentation.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Base class for the view models both apps share. It does the one thing a view model needs from a
 * framework: a coroutine scope that lives as long as the screen, and a [clear] that ends it.
 *
 * Each platform decides who calls [clear]:
 *  - Android: its own `androidx.lifecycle.ViewModel` holds the shared one and calls [clear] in `onCleared()`.
 *  - iOS: the Swift `ObservableObject` that wraps it calls [clear] in `deinit`.
 *
 * Everything runs on the main thread, so state changes are never raced and callbacks to the UI need no hopping.
 */
abstract class SharedViewModel internal constructor(protected val scope: CoroutineScope) {

    /** Stops everything this view model started. Safe to call more than once. */
    fun clear() {
        scope.cancel()
        onCleared()
    }

    protected open fun onCleared() {}

    /** For Swift: calls [onState] now and on every change, on the main thread. Dispose the handle when done. */
    protected fun <T> observe(flow: StateFlow<T>, onState: (T) -> Unit): DisposableHandle {
        val job = scope.launch { flow.collect { onState(it) } }
        return DisposableHandle { job.cancel() }
    }
}

internal fun vmScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
