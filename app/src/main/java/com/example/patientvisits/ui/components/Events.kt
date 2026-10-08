package com.example.patientvisits.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow


@Composable
fun <T> CollectEvents(events: Flow<T>, onEvent: (T) -> Unit) {
    val latest by rememberUpdatedState(onEvent)
    LaunchedEffect(events) {
        events.collect { latest(it) }
    }
}
