import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val distributionVersion = providers
    .gradleProperty("packageVersion")
    .orElse("1.0.0")

val windowsPackageVersion = providers
    .gradleProperty("msiVersion")
    .orElse("1.0.99")

val linuxPackageVersion = providers
    .gradleProperty("debVersion")
    .orElse("1.0.0")

sourceSets {
    main {
        resources.srcDir("resources")
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.feature.formulaPanel)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.preferences)

    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.nucleus.global.hotkey)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.composenativetray)

    runtimeOnly(libs.slf4j.nop)
}

compose.desktop {
    application {
        mainClass = "com.formuladock.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Deb,
            )

            packageName = "FormulaDock"
            packageVersion = distributionVersion.get()

            includeAllModules = true

            windows {
                iconFile.set(project.file("resources/icon.ico"))

                menuGroup = "FormulaDock"
                shortcut = true
                perUserInstall = true
                dirChooser = true

                upgradeUuid = "8d7ef52a-c590-4168-8e7c-8d9c3f73f808"

                msiPackageVersion = windowsPackageVersion.get()
            }

            linux {
                iconFile.set(project.file("resources/icon.png"))

                packageName = "formuladock"
                menuGroup = "Utility"
                appCategory = "utils"

                debPackageVersion = linuxPackageVersion.get()
            }
        }
    }
}