plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.kotlin.plugin)
    implementation(libs.agp)
    implementation(libs.kotlin.compose.plugin)
    implementation(libs.compose.plugin)
    implementation(libs.vanniktech.publish.plugin)
    implementation(libs.mokkery.plugin)
}
