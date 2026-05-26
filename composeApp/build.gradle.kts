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

val generateSecretsXcconfig by tasks.registering {
    description = "Generate iosApp/Configuration/Secrets.xcconfig from local.properties"
    val outputFile = rootProject.file("iosApp/Configuration/Secrets.xcconfig")
    val url = localProp("SUPABASE_URL")
    val key = localProp("SUPABASE_ANON_KEY")
    inputs.property("supabaseUrl", url)
    inputs.property("supabaseKey", key)
    outputs.file(outputFile)
    doLast {
        // xcconfig treats // as a comment — escape with /$()/
        val escapedUrl = url.replace("//", "/\$()/")
        outputFile.writeText(
            """
            |// Auto-generated from local.properties — do not edit manually.
            |SUPABASE_URL = $escapedUrl
            |SUPABASE_ANON_KEY = $key
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
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    tasks.matching { it.name.startsWith("compileKotlinIos") }.configureEach {
        dependsOn(generateSecretsXcconfig)
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
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
            implementation(libs.kotlinx.datetime)
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
        create("production") {
            dimension = "environment"
            resValue("string", "app_name", "Bookskmp")
            // Inherits defaultConfig credentials — no overrides needed.
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            resValue("string", "app_name", "Books Staging")
            buildConfigField("String", "SUPABASE_URL", "\"${envOrLocalProp("STAGING_SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${envOrLocalProp("STAGING_SUPABASE_ANON_KEY")}\"")
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
