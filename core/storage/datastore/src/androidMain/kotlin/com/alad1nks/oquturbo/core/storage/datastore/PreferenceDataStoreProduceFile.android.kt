package com.alad1nks.oquturbo.core.storage.datastore

import okio.Path
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.core.scope.Scope

actual val Scope.PreferenceDataStoreProduceFile: Path
    get() = androidContext().filesDir.resolve(STORAGE_DATA_STORE_FILE_NAME).absolutePath.toPath()
