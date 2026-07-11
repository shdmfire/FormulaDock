rootProject.name = "FormulaDock"
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

include(":androidApp")
include(":desktopApp")
include(":shared")
include(":core:database")
include(":core:data")
include(":core:model")
include(":core:designsystem")
include(":core:i18n")
include(":core:formula-engine")
include(":feature:formula-list")
include(":feature:formula-run")
include(":feature:formula-editor")
include(":feature:formula-panel")
include(":core:domain")
include(":core:navigation")
include(":feature:formula-history")
include(":core:preferences")
include(":feature:preferences")
include(":core:formula-io")
include(":feature:formula-io")
