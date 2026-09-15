package com.alad1nks.symbolcount.shared

import com.alad1nks.oquturbo.core.data.model.GameId
import com.alad1nks.oquturbo.core.data.model.GameModeId
import com.alad1nks.oquturbo.core.data.repository.GameActivityRepository
import com.alad1nks.oquturbo.core.storage.common.AppPreferences
import com.alad1nks.oquturbo.core.storage.common.Storage
import com.alad1nks.oquturbo.core.storage.common.di.StorageCommonModule
import com.alad1nks.oquturbo.core.storage.datastore.di.StorageDataStoreModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.dsl.koinApplication
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SymbolCountDesktopStorageTest {
    @Test
    fun symbolCountUsesAProductLocalDurablePath() {
        val userHome = File("build/test-home").absolutePath
        val symbolCountPath = symbolCountDataStorePath(userHome)
        val genericOquTurboPath = File(System.getProperty("java.io.tmpdir"), "oquturbo.preferences_pb").absolutePath

        assertEquals(
            File(userHome, ".symbolcount/$SYMBOL_COUNT_DATA_STORE_FILE_NAME").absolutePath,
            symbolCountPath,
        )
        assertNotEquals(genericOquTurboPath, symbolCountPath)
        assertNotSame(StorageDataStoreModule, getPlatformModules().single())
    }

    @Test
    fun symbolCountModulePersistsPreferencesThroughItsSingletonDataStore() =
        runTest {
            val userHome = Files.createTempDirectory("symbolcount-storage-test").toFile()
            val dataStoreFile = File(symbolCountDataStorePath(userHome.absolutePath))
            val koinApplication =
                startKoin {
                    modules(symbolCountStorageDataStoreModule(userHome.absolutePath))
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

    @Test
    fun completedSessionSurvivesSeparateJvmRestartWithoutTouchingSiblingFiles() {
        val home = Files.createTempDirectory("symbolcount-restart").toFile()
        val sibling = File(home, ".numbertrail/numbertrail.preferences_pb")
        sibling.parentFile.mkdirs()
        sibling.writeText("unrelated sibling sentinel")
        val urls =
            generateSequence(javaClass.classLoader) { it.parent }
                .filterIsInstance<URLClassLoader>().flatMap { it.urLs.asSequence() }
                .map { File(it.toURI()).path }.toList()
        val classpath =
            (urls + System.getProperty("java.class.path").split(File.pathSeparator))
                .distinct().joinToString(File.pathSeparator)

        fun run(mode: String) {
            val log = File(home, "$mode.log")
            val process =
                ProcessBuilder(
                    File(System.getProperty("java.home"), "bin/java").path,
                    "-cp",
                    classpath,
                    "com.alad1nks.symbolcount.shared.SymbolCountStorageProcess",
                    home.absolutePath,
                    mode,
                ).redirectErrorStream(true).redirectOutput(log).start()
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "Storage child did not finish")
            assertEquals(0, process.exitValue(), log.readText())
        }
        run("write")
        run("read")
        assertEquals("unrelated sibling sentinel", sibling.readText())
    }

    private companion object {
        const val TEST_RECORD_KEY = "symbol_count_test_record"
    }
}

/** A fresh process proves production DataStore bytes, repository records and totals survive process loss. */
object SymbolCountStorageProcess {
    @JvmStatic
    fun main(args: Array<String>) =
        runBlocking<Unit> {
            val app =
                koinApplication {
                    modules(StorageCommonModule, symbolCountStorageDataStoreModule(args[0]))
                }
            try {
                val repo = GameActivityRepository(app.koin.get<Storage>())
                if (args[1] == "write") {
                    check(repo.observeSessions().first().isEmpty())
                    repo.recordCompletedSession(
                        GameId.SymbolCount,
                        GameModeId.SymbolCountCount,
                        score = 7,
                        correctAnswers = 7,
                        durationMillis = 4321,
                        isNewRecord = true,
                    )
                } else {
                    val session = repo.observeSessions().first().single()
                    check(session.game == GameId.SymbolCount && session.score == 7 && session.durationMillis == 4321L)
                    check(repo.observeRecords().first().single().score == 7)
                    check(repo.observeTotals().first().correctAnswers == 7L)
                }
            } finally {
                app.close()
            }
        }
}
