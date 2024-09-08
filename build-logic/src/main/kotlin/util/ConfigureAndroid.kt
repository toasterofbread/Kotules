package util

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

fun BaseExtension.configureAndroid(project: Project) {
    defaultConfig {
        versionCode = project.libCatalog.version("project.version.inc").toInt()
        versionName = project.libCatalog.version("project.version.name")

        targetSdk = project.libCatalog.version("android.sdk.target").toInt()
        minSdk = project.libCatalog.version("android.sdk.min").toInt()

        if (this is ApplicationExtension) {
            applicationId = "dev.toastbits." + project.libCatalog.version("project.name")
        }
    }

    if (this is CommonExtension<*, *, *, *, *, *>) {
        compileSdk = project.libCatalog.version("android.sdk.compile").toInt()
    }

    namespace = project.getCurrentPackage()
}
