package com.alad1nks.oquturbo.core.data.model

data class ReminderContent(val languageCode: String, val title: String, val body: String) {
    init {
        require(languageCode in setOf("en", "ru", "kk"))
        require(title.isNotBlank() && body.isNotBlank())
    }
}

data class ReminderSchedule(val minutesOfDay: Int, val content: ReminderContent) {
    init {
        require(minutesOfDay in 0..1439)
    }
}
