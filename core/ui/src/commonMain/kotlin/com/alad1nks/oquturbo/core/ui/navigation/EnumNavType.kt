package com.alad1nks.oquturbo.core.ui.navigation

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

inline fun <reified T : Enum<T>> enumNavType(): NavType<T> =
    object : NavType<T>(isNullableAllowed = false) {
        private val valuesByName = enumValues<T>().associateBy(Enum<T>::name)

        override fun put(
            bundle: SavedState,
            key: String,
            value: T,
        ) {
            bundle.write { putString(key, value.name) }
        }

        override fun get(
            bundle: SavedState,
            key: String,
        ): T? = bundle.read { getStringOrNull(key) }?.let(::parseValue)

        override fun parseValue(value: String): T =
            requireNotNull(valuesByName[value]) {
                "Unknown ${T::class.simpleName} value: $value"
            }

        override fun serializeAsValue(value: T): String = value.name
    }
