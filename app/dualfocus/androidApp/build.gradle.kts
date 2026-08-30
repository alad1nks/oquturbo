import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val releaseSigningStoreFile =
    System.getenv("SIGNING_STORE_FILE")
        ?.takeIf(String::isNotBlank)
        ?.let(::file)
        ?: file("../../../release-key.jks")
val releaseSigningStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
val releaseSigningKeyAlias = System.getenv("SIGNING_KEY_ALIAS")
val releaseSigningKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
val hasReleaseSigning =
    releaseSigningStoreFile.isFile &&
        !releaseSigningStorePassword.isNullOrBlank() &&
        !releaseSigningKeyAlias.isNullOrBlank() &&
        !releaseSigningKeyPassword.isNullOrBlank()

kotlin {
    target {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }
    dependencies {
        implementation(libs.androidx.activity.compose)
        implementation(projects.app.dualfocus.shared)
    }
}

android {
    namespace = "com.alad1nks.dualfocus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.alad1nks.dualfocus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseSigningStoreFile
                storePassword = releaseSigningStorePassword
                keyAlias = releaseSigningKeyAlias
                keyPassword = releaseSigningKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
