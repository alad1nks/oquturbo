package com.alad1nks.oquturbo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform