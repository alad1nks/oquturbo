package com.alad1nks.oquturbo.shared

import com.alad1nks.oquturbo.resources.reminderResourceContent
import java.nio.file.Path
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ReminderResourceSnapshotTest {
    @Test fun explicitLanguageSnapshotsMatchCanonicalResourcesWithoutMutatingLocale() {
        val root =
            generateSequence(Path.of("").toAbsolutePath()) {
                it.parent
            }.first { it.resolve("settings.gradle.kts").toFile().isFile }
        val original = Locale.getDefault()
        for ((code, folder) in listOf("en" to "values", "ru" to "values-ru", "kk" to "values-kk")) {
            val nodes =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    root.resolve("resources/src/commonMain/composeResources/$folder/strings.xml").toFile(),
                ).getElementsByTagName("string")
            val values =
                (0 until nodes.length).associate {
                    i ->
                    val e = nodes.item(i) as org.w3c.dom.Element
                    e.getAttribute("name") to e.textContent
                }
            val snapshot = reminderResourceContent(code)
            assertEquals(values["reminder_notification_title"], snapshot.title)
            assertEquals(values["reminder_notification_body"], snapshot.body)
            assertEquals(values["reminder_channel_name"], snapshot.channelName)
            assertEquals(values["reminder_channel_description"], snapshot.channelDescription)
            assertEquals(original, Locale.getDefault())
        }
        assertFails { reminderResourceContent("fr") }
    }
}
