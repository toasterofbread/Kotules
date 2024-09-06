package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.Kotule
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path

actual interface KotuleLoader<T: Kotule> {
    fun loadFromJar(jarPath: String, implementationClass: String): T
}

@Suppress("NewApi")
fun loadKotuleFromJar(jarPath: String, implementationClass: String): Any {
    val classLoader: URLClassLoader =
        URLClassLoader(
            arrayOf(URL("file://$jarPath")),
            KotuleLoader::class.java.classLoader
        )

    val cls: Class<*> = Class.forName(implementationClass, true, classLoader)
    return cls.getConstructor().newInstance()
}
