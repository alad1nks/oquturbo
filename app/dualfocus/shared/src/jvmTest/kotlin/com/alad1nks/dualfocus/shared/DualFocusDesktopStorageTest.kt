package com.alad1nks.dualfocus.shared

import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DualFocusDesktopStorageTest {
    @Test
    fun dualFocusUsesAProductLocalDurablePath() {
        val userHome = File("build/test-home").absolutePath
        val dualFocusPath = dualFocusDataStorePath(userHome)
        val genericOquTurboPath = File(System.getProperty("java.io.tmpdir"), "oquturbo.preferences_pb").absolutePath

        assertEquals(
            File(userHome, ".dualfocus/$DUAL_FOCUS_DATA_STORE_FILE_NAME").absolutePath,
            dualFocusPath,
        )
        assertNotEquals(genericOquTurboPath, dualFocusPath)
        assertNotSame(StorageDataStoreModule, getPlatformModules().single())
    }

    @Test
    fun dualFocusModulePersistsPreferencesThroughItsSingletonDataStore() =
        runTest {
            val userHome = Files.createTempDirectory("dualfocus-storage-test").toFile()
            val dataStoreFile = File(dualFocusDataStorePath(userHome.absolutePath))
            val koinApplication =
                startKoin {
                    modules(dualFocusStorageDataStoreModule(userHome.absolutePath))
                }
            try {
                val firstPreferences = koinApplication.koin.get<AppPreferences>()
                val secondPreferences = koinApplication.koin.get<AppPreferences>()

                assertSame(firstPreferences, secondPreferences)
                firstPreferences.setInt(TEST_RECORD_KEY, 7)

                assertEquals(7, secondPreferences.getInt(TEST_RECORD_KEY).first())
                assertTrue(dataStoreFile.isFile)
                assertTrue(dataStoreFile.parentFile.isDirectory)
            } finally {
                koinApplication.close()
            }
        }

    private companion object {
        const val TEST_RECORD_KEY = "dual_focus_test_record"
    }
}
