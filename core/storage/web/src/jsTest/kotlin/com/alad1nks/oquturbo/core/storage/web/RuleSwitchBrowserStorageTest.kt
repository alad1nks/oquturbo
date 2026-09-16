package com.alad1nks.oquturbo.core.storage.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RuleSwitchBrowserStorageTest {
    @Test
    fun sameOriginProductsStayIsolatedAndReopenedNamespaceRetainsSavedValue() =
        runTest {
            val key = "rule-switch-isolation-test"
            val own = AppPreferencesImpl("ruleswitch")
            val sibling = AppPreferencesImpl("numbertrail")
            try {
                localStorage.setItem(key, "hub")
                sibling.setString(key, "sibling")
                localStorage.removeItem("ruleswitch:$key")
                assertNull(own.getString(key).first())
                own.setString(key, "completed result")
                assertEquals("hub", localStorage.getItem(key))
                assertEquals("sibling", sibling.getString(key).first())
                assertEquals("completed result", AppPreferencesImpl("ruleswitch").getString(key).first())
            } finally {
                localStorage.removeItem(key)
                localStorage.removeItem("ruleswitch:$key")
                localStorage.removeItem("numbertrail:$key")
            }
        }
}
