import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    android {
        namespace = "com.alad1nks.oquturbo.resources"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
        }
    }
}

// Four background strings, generated from the same XML as Compose resources. No runtime locale mutation.
abstract class GenerateReminderContent : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringFiles: ConfigurableFileCollection

    @get:Input
    abstract val generatedPackage: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        fun quoted(value: String): String =
            buildString {
                append('"')
                value.forEach { char ->
                    when (char) {
                        '\\' -> append("\\\\")
                        '"' -> append("\\\"")
                        '$' -> append("\\$")
                        '\n' -> append("\\n")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        else ->
                            if (char.code < 32) {
                                append(
                                    "\\u" + char.code.toString(16).padStart(4, '0'),
                                )
                            } else {
                                append(char)
                            }
                    }
                }
                append('"')
            }
        val keys =
            listOf(
                "reminder_notification_title",
                "reminder_notification_body",
                "reminder_channel_name",
                "reminder_channel_description",
            )
        val factory =
            javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
        val byFolder = stringFiles.files.associateBy { it.parentFile.name }
        val branches =
            listOf("en" to "values", "ru" to "values-ru", "kk" to "values-kk").map { (code, folder) ->
                val xml = factory.newDocumentBuilder().parse(requireNotNull(byFolder[folder]))
                val nodes = xml.getElementsByTagName("string")
                val values =
                    (0 until nodes.length).associate { i ->
                        val node = nodes.item(i) as org.w3c.dom.Element
                        node.getAttribute("name") to node.textContent
                    }
                val content =
                    keys.map { key ->
                        requireNotNull(values[key]) { "$folder: missing $key" }.also {
                            require(it.isNotBlank()) { "$folder: blank $key" }
                        }
                    }
                "    ${quoted(code)} -> ReminderResourceContent(${content.joinToString(", ", transform = ::quoted)})"
            }
        val packageName = generatedPackage.get()
        val output = outputDirectory.file("${packageName.replace('.', '/')}/ReminderResourceContent.kt").get().asFile
        output.parentFile.mkdirs()
        output.writeText(
            """
            |package $packageName
            |
            |data class ReminderResourceContent(
                |    val title: String, val body: String, val channelName: String, val channelDescription: String,
                |)
            |
            |fun reminderResourceContent(languageCode: String): ReminderResourceContent = when (languageCode) {
            |${branches.joinToString("\n")}
            |    else -> error("Unsupported reminder language")
            |}
            |
            """.trimMargin(),
        )
    }
}

val generateReminderContent by tasks.registering(GenerateReminderContent::class) {
    generatedPackage.set("com.alad1nks.oquturbo.resources")
    stringFiles.from(
        listOf("values", "values-ru", "values-kk").map { "src/commonMain/composeResources/$it/strings.xml" },
    )
    outputDirectory.set(layout.buildDirectory.dir("generated/reminderContent/commonMain"))
}
kotlin.sourceSets.commonMain { kotlin.srcDir(generateReminderContent.flatMap { it.outputDirectory }) }

val generateReminderContentFixture by tasks.registering(GenerateReminderContent::class) {
    generatedPackage.set("com.alad1nks.oquturbo.resources.fixture")
    stringFiles.from(
        listOf("values", "values-ru", "values-kk").map {
            "src/jvmTest/resources/reminder-generator/$it/strings.xml"
        },
    )
    outputDirectory.set(layout.buildDirectory.dir("generated/reminderContent/jvmTest"))
}
kotlin.sourceSets.named("jvmTest") {
    kotlin.srcDir(generateReminderContentFixture.flatMap { it.outputDirectory })
    dependencies { implementation(libs.kotlin.test) }
}
