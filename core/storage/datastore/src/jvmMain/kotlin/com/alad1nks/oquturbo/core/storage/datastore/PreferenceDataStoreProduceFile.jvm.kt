package com.alad1nks.oquturbo.core.storage.datastore

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.scope.Scope
import java.io.File

actual val Scope.PreferenceDataStoreProduceFile: Path
    get() = File(System.getProperty("java.io.tmpdir"), STORAGE_DATA_STORE_FILE_NAME).absolutePath.toPath()
