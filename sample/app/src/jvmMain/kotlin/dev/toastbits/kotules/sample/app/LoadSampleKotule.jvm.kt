package dev.toastbits.kotules.sample.app

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

actual suspend fun loadSampleKotule(): SampleKotule {
    println("Loading extension from jar at ${SampleConfig.JVM_EXTENSION_FILE}")

    val jarPath: Path = Path(System.getProperty("user.dir"))
        .resolve("../../")
        .resolve(SampleConfig.JVM_EXTENSION_FILE)
        .normalize()

    return SampleKotule_Loader.loadFromJarFile(jarPath.pathString, SampleConfig.EXTENSION_IMPL_CLASS)
}
