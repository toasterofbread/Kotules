package util

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

fun BaseExtension.configureAndroid(project: Project) {
    defaultConfig {
        versionCode = project.libs.version("project.version.inc").toInt()
        versionName = project.libs.version("project.version")

        targetSdk = project.libs.version("android.sdk.target").toInt()
        minSdk = project.libs.version("android.sdk.min").toInt()

        if (this is ApplicationExtension) {
            applicationId = "dev.toastbits." + project.libs.version("project.name")
        }
    }

    if (this is CommonExtension<*, *, *, *, *, *>) {
        compileSdk = project.libs.version("android.sdk.compile").toInt()
    }

    namespace = project.getCurrentPackage()
}
