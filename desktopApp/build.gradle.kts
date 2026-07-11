import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

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
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "FormulaDock"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("resources/icon.icns"))
            }

            windows {
                iconFile.set(project.file("resources/icon.ico"))
                menuGroup = "FormulaDock"
                shortcut = true
            }

            linux {
                iconFile.set(project.file("resources/icon.png"))
            }
        }
    }
}