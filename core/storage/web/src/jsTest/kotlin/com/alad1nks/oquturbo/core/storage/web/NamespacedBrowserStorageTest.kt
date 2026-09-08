package com.alad1nks.oquturbo.core.storage.web

import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.web.di.StorageWebModule
import com.alad1nks.oquturbo.core.storage.web.di.storageWebModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NamespacedBrowserStorageTest {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun failedDurableWriteDoesNotPublishAnUnsavedRecord() =
        runTest {
            val key = "numbertrail-failed-write-test"
            localStorage.removeItem("numbertrail:$key")
            val preferences = AppPreferencesImpl("numbertrail")
            var observed: String? = null
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                preferences.getString(key).collect { observed = it }
            }
            val prototype = js("Storage.prototype")
            val original = prototype.setItem
            try {
                prototype.setItem = { _: dynamic, _: dynamic -> throw IllegalStateException("quota exceeded") }
                assertFailsWith<IllegalStateException> { preferences.setString(key, "unsaved record") }
                runCurrent()
                assertNull(observed)
                assertNull(localStorage.getItem("numbertrail:$key"))
            } finally {
                prototype.setItem = original
                localStorage.removeItem("numbertrail:$key")
            }
        }

    @Test
    fun defaultFactoryPreservesLegacyKeysAndNewProductSurvivesReloadWithoutCrossWrites() =
        runTest {
            val key = "numbertrail-isolation-test"
            val legacyApp = koinApplication { modules(StorageWebModule) }
            val productApp = koinApplication { modules(storageWebModule("numbertrail")) }
            try {
                localStorage.setItem(key, "legacy")
                localStorage.removeItem("numbertrail:$key")
                val legacy = legacyApp.koin.get<AppPreferences>()
                val product = productApp.koin.get<AppPreferences>()
                assertEquals("legacy", legacy.getString(key).first())
                assertNull(product.getString(key).first())
                product.setString(key, "new product")
                assertEquals("legacy", localStorage.getItem(key))
                assertEquals("new product", localStorage.getItem("numbertrail:$key"))
                legacy.setString(key, "updated legacy")
                val reopened = koinApplication { modules(storageWebModule("numbertrail")) }
                try {
                    assertEquals("new product", reopened.koin.get<AppPreferences>().getString(key).first())
                } finally {
                    reopened.close()
                }
                product.setBoolean(key, true)
                assertEquals(true, product.getBoolean(key).first())
                assertEquals("true", localStorage.getItem("numbertrail:$key"))
                product.setInt(key, 48)
                assertEquals(48, product.getInt(key).first())
                assertEquals("48", localStorage.getItem("numbertrail:$key"))
                assertEquals("updated legacy", localStorage.getItem(key))
            } finally {
                localStorage.removeItem(key)
                localStorage.removeItem("numbertrail:$key")
                legacyApp.close()
                productApp.close()
            }
        }
}
