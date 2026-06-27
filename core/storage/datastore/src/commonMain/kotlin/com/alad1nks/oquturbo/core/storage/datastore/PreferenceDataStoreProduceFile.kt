package com.alad1nks.oquturbo.core.storage.datastore

import okio.Path
import org.koin.core.scope.Scope

expect val Scope.PreferenceDataStoreProduceFile: Path
