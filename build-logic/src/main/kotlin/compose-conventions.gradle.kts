import util.libs
import util.version

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("kmp-conventions")
    id("org.jetbrains.compose")
}

val projectName: String = libs.version("project.name")

kotlin {
    sourceSets {
        all {
            languageSettings.apply {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("androidx.compose.foundation.ExperimentalFoundationApi")
                optIn("androidx.compose.foundation.layout.ExperimentalLayoutApi")
                optIn("androidx.compose.material3.ExperimentalMaterial3Api")
                optIn("androidx.compose.material.ExperimentalMaterialApi")
                optIn("androidx.compose.ui.ExperimentalComposeUiApi")
            }
        }

        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.materialIconsExtended)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "dev.toastbits.$projectName.application.MainKt"
        }
    }

    resources {
        publicResClass = true
    }
}
