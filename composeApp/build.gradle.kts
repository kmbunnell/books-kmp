import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { localProps.load(it) }
}

fun localProp(key: String): String =
    localProps.getProperty(key)
        ?: error("Missing required local.properties key: $key")

fun envOrLocalProp(key: String): String =
    System.getenv(key)
        ?: localProps.getProperty(key)
        ?: error("Missing $key — add to local.properties or set as env var")

fun envOrLocalPropOrEmpty(key: String): String = System.getenv(key) ?: localProps.getProperty(key) ?: ""

fun escapeKotlinStringLiteral(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\${'$'}")

val generateSecretsXcconfig by tasks.registering {
    description = "Generate iosApp/Configuration/Secrets.xcconfig from local.properties"
    val outputFile = rootProject.file("iosApp/Configuration/Secrets.xcconfig")
    val url = envOrLocalProp("SUPABASE_URL")
    val key = envOrLocalProp("SUPABASE_ANON_KEY")
    val groqKey = envOrLocalPropOrEmpty("GROQ_API_KEY")
    inputs.property("supabaseUrl", url)
    inputs.property("supabaseKey", key)
    inputs.property("groqApiKey", groqKey)
    outputs.file(outputFile)
    doLast {
        // xcconfig treats // as a comment — escape with /$()/
        val escapedUrl = url.replace("//", "/\$()/")
        outputFile.writeText(
            """
            |// Auto-generated from local.properties — do not edit manually.
            |SUPABASE_URL = $escapedUrl
            |SUPABASE_ANON_KEY = $key
            |GROQ_API_KEY = $groqKey
            """.trimMargin() + "\n",
        )
    }
}

val generateStagingSecretsXcconfig by tasks.registering {
    description = "Generate iosApp/Configuration/StagingSecrets.xcconfig from local.properties"
    val outputFile = rootProject.file("iosApp/Configuration/StagingSecrets.xcconfig")
    val url = envOrLocalPropOrEmpty("STAGING_SUPABASE_URL")
    val key = envOrLocalPropOrEmpty("STAGING_SUPABASE_ANON_KEY")
    val groqKey = envOrLocalPropOrEmpty("GROQ_API_KEY")
    inputs.property("stagingSupabaseUrl", url)
    inputs.property("stagingSupabaseKey", key)
    inputs.property("groqApiKey", groqKey)
    outputs.file(outputFile)
    doLast {
        // xcconfig treats // as a comment — escape with /$()/
        val escapedUrl = url.replace("//", "/\$()/")
        outputFile.writeText(
            """
            |// Auto-generated from local.properties — do not edit manually.
            |SUPABASE_URL = $escapedUrl
            |SUPABASE_ANON_KEY = $key
            |GROQ_API_KEY = $groqKey
            """.trimMargin() + "\n",
        )
    }
}

// "staging" or "production" (default). Set via `-PdesktopEnv=staging` or `DESKTOP_ENV=staging` env var.
val desktopEnv = (project.findProperty("desktopEnv") as String?) ?: System.getenv("DESKTOP_ENV") ?: "production"

val generateDesktopConfig by tasks.registering {
    description = "Generate desktop Config.kt from local.properties (output under build/, never committed)"
    val outputDir = layout.buildDirectory.dir("generated/source/desktopConfig/kotlin")
    val isStaging = desktopEnv == "staging"
    val url = if (isStaging) envOrLocalProp("STAGING_SUPABASE_URL") else envOrLocalProp("SUPABASE_URL")
    val key = if (isStaging) envOrLocalProp("STAGING_SUPABASE_ANON_KEY") else envOrLocalProp("SUPABASE_ANON_KEY")
    val groqKey = envOrLocalPropOrEmpty("GROQ_API_KEY")
    inputs.property("desktopEnv", desktopEnv)
    inputs.property("supabaseUrl", url)
    inputs.property("supabaseKey", key)
    inputs.property("groqApiKey", groqKey)
    outputs.dir(outputDir)
    val escapedUrl = escapeKotlinStringLiteral(url)
    val escapedKey = escapeKotlinStringLiteral(key)
    val escapedGroqKey = escapeKotlinStringLiteral(groqKey)
    val escapedEnv = escapeKotlinStringLiteral(desktopEnv)
    doLast {
        val dir = outputDir.get().asFile
        dir.mkdirs()
        dir.resolve("Config.kt").writeText(
            """
            |// Auto-generated from local.properties — do not edit manually or commit.
            |object Config {
            |    const val SUPABASE_URL: String = "$escapedUrl"
            |    const val SUPABASE_ANON_KEY: String = "$escapedKey"
            |    const val GROQ_API_KEY: String = "$escapedGroqKey"
            |    const val ENVIRONMENT: String = "$escapedEnv"
            |    const val IS_STAGING: Boolean = $isStaging
            |}
            """.trimMargin() + "\n",
        )
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            linkerOpts("-lsqlite3")
        }
    }

    jvm("desktop")

    tasks.matching { it.name.startsWith("compileKotlinIos") }.configureEach {
        dependsOn(generateSecretsXcconfig)
        dependsOn(generateStagingSecretsXcconfig)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material3.windowSizeClass)
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptive.layout)
            implementation(libs.compose.material3.adaptive.navigation)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.composeAuth)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.navigation.compose)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
            implementation(libs.mlkit.barcode.scanning)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(compose.desktop.currentOs)
            }
            kotlin.srcDir(generateDesktopConfig)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(projects.sharedTesting)
        }
        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

android {
    namespace = "com.example.books_kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.books_kmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${envOrLocalProp("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${envOrLocalProp("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${envOrLocalPropOrEmpty("GROQ_API_KEY")}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    flavorDimensions += "environment"

    productFlavors {
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "Books Staging")
            buildConfigField("String", "SUPABASE_URL", "\"${envOrLocalProp("STAGING_SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${envOrLocalProp("STAGING_SUPABASE_ANON_KEY")}\"")
        }
        create("production") {
            dimension = "environment"
            resValue("string", "app_name", "Bookskmp")
            // Inherits defaultConfig credentials — no overrides needed.
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            // Unit tests run against the debug variant only; release tests are not meaningful here.
            all { test -> if (test.name.contains("release", ignoreCase = true)) test.enabled = false }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

compose.desktop {
    application {
        mainClass = "MainKt"
    }
}
