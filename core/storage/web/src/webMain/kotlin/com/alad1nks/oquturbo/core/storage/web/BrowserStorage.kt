package com.alad1nks.oquturbo.core.storage.web

internal external interface BrowserStorage {
    fun getItem(key: String): String?

    fun setItem(key: String, value: String)

    fun removeItem(key: String)

    fun clear()
}
