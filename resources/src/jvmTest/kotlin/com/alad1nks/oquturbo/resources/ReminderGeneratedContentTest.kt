package com.alad1nks.oquturbo.resources

import com.alad1nks.oquturbo.resources.fixture.reminderResourceContent
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderGeneratedContentTest {
    @Test
    fun generatedKotlinPreservesXmlEntitiesUnicodeAndEscapesForEveryLocale() {
        for (language in listOf("en", "ru", "kk")) {
            val content = reminderResourceContent(language)
            assertEquals("Quote \" and dollar \$value", content.title)
            assertEquals("Backslash \\ and line\nnext", content.body)
            assertEquals("Қазақша & Русский", content.channelName)
            assertEquals("Tabs\tstay and apostrophe '", content.channelDescription)
        }
    }
}
