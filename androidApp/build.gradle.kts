import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersion = providers
    .gradleProperty("appVersion")
    .orElse("1.0.0")

val appVersionCode = providers
    .gradleProperty("appVersionCode")
    .map(String::toInt)
    .orElse(1)

val signRelease = providers
    .gradleProperty("signRelease")
    .map(String::toBoolean)
    .orElse(false)

fun requiredEnvironmentVariable(name: String): String =
    System.getenv(name)
        ?: error("Missing required environment variable: $name")

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.formulaPanel)
    implementation(projects.feature.formulaIo)
    implementation(projects.core.preferences)
    implementation(projects.core.i18n)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.formuladock"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.formuladock"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        versionCode = appVersionCode.get()
        versionName = appVersion.get()
    }

    signingConfigs {
        if (signRelease.get()) {
            create("release") {
                storeFile = file(requiredEnvironmentVariable("KEYSTORE_PATH"))
                storePassword = requiredEnvironmentVariable("KEYSTORE_PASSWORD")
                keyAlias = requiredEnvironmentVariable("KEY_ALIAS")
                keyPassword = requiredEnvironmentVariable("KEY_PASSWORD")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false

            if (signRelease.get()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}