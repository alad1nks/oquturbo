rootProject.name = "OquTurbo"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app:kenkoz:androidApp")
include(":app:kenkoz:desktopApp")
include(":app:kenkoz:shared")
include(":app:kenkoz:webApp")
include(":app:oquturbo:androidApp")
include(":app:oquturbo:desktopApp")
include(":app:oquturbo:shared")
include(":app:oquturbo:webApp")
include(":app:sansprint:androidApp")
include(":app:sansprint:desktopApp")
include(":app:sansprint:shared")
include(":app:sansprint:webApp")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:storage:common")
include(":core:storage:datastore")
include(":core:storage:web")
include(":feature:main")
include(":feature:kenkozgame")
include(":feature:kenkozgamemenu")
include(":feature:remembernumber")
include(":feature:remembernumbermenu")
include(":resources")
