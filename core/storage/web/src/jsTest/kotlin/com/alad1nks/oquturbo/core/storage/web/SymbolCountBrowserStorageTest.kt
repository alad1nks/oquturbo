package com.alad1nks.oquturbo.core.storage.web

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SymbolCountBrowserStorageTest {
    @Test
    fun sameOriginProductsStayIsolatedAndReopenedNamespaceRetainsSavedValue() =
        runTest {
            val key = "symbol-count-isolation-test"
            val own = AppPreferencesImpl("symbolcount")
            val sibling = AppPreferencesImpl("numbertrail")
            try {
                localStorage.setItem(key, "hub")
                sibling.setString(key, "sibling")
                localStorage.removeItem("symbolcount:$key")
                assertNull(own.getString(key).first())
                own.setString(key, "completed result")
                assertEquals("hub", localStorage.getItem(key))
                assertEquals("sibling", sibling.getString(key).first())
                assertEquals("completed result", AppPreferencesImpl("symbolcount").getString(key).first())
            } finally {
                localStorage.removeItem(key)
                localStorage.removeItem("symbolcount:$key")
                localStorage.removeItem("numbertrail:$key")
            }
        }
}
