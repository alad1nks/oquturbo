package com.alad1nks.oquturbo.core.data.reminders

/** Optional diagnostic observations, never evidence of a completed reminder action. */
fun interface ReminderActionObserver {
    fun record(event: String, fields: Map<String, String>)
}

fun ReminderActionObserver?.observeAction(event: String, fields: Map<String, String> = emptyMap()) {
    // Only the observer is guarded; callers must invoke real actions outside this boundary.
    runCatching { this?.record(event, fields) }
}
