import com.github.takahirom.roborazzi.AnnotationFilter
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.roborazzi)
}

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.feature.baspagame)
    implementation(projects.feature.baspagamemenu)
    implementation(projects.feature.games)
    implementation(projects.feature.home)
    implementation(projects.feature.kenkozgame)
    implementation(projects.feature.kenkozgamemenu)
    implementation(projects.feature.memorygrid)
    implementation(projects.feature.profile)
    implementation(projects.feature.remembernumber)
    implementation(projects.feature.remembernumbermenu)
    implementation(projects.feature.stats)

    testImplementation(libs.compose.components.resources)
    testImplementation(libs.compose.material3)
    testImplementation(libs.compose.ui.tooling.preview)
    testImplementation(projects.core.designsystem)
    testImplementation(projects.resources)
    testImplementation(libs.roborazzi.compose.desktop.preview.scanner.support)
    testImplementation(libs.composable.preview.scanner)
    testImplementation(libs.junit4)
    testRuntimeOnly(compose.desktop.currentOs)
}

@OptIn(ExperimentalRoborazziApi::class)
roborazzi {
    outputDir.set(file("src/test/screenshots"))
    separateOutputDirs.set(true)

    generateComposePreviewDesktopTests {
        enable = true
        packages = listOf("com.alad1nks.oquturbo")
        annotationFilter =
            AnnotationFilter.Include(
                "com.alad1nks.oquturbo.core.ui.preview.ScreenshotPreview",
            )
        includePrivatePreviews = true
        generatedTestClassCount = 1
    }
}
