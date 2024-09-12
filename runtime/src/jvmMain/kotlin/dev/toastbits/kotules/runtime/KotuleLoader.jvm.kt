package dev.toastbits.kotules.runtime

import java.io.ByteArrayInputStream
import dev.toastbits.kotules.core.Kotule
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarEntry
import java.util.jar.JarInputStream

actual interface KotuleLoader<T: Kotule> {
    fun loadFromJarFile(jarPath: String, implementationClass: String): T
    fun loadFromJarBytes(jarBytes: ByteArray, implementationClass: String): T
}

fun loadKotuleFromClassLoader(classLoader: ClassLoader, implementationClass: String): Any {
    val cls: Class<*> = Class.forName(implementationClass, true, classLoader)
    return cls.getConstructor().newInstance()
}

fun loadKotuleFromJarFile(jarPath: String, implementationClass: String): Any =
    loadKotuleFromClassLoader(URLClassLoader(arrayOf(URL("file://$jarPath"))), implementationClass)

fun loadKotuleFromJarBytes(jarBytes: ByteArray, implementationClass: String): Any =
    loadKotuleFromClassLoader(InMemoryJarClassLoader(jarBytes), implementationClass)

class InMemoryJarClassLoader(jarBytes: ByteArray): ClassLoader() {
    private val classes: MutableMap<String, ByteArray> = loadClassesFromJar(jarBytes)

    override fun findClass(name: String): Class<*> {
        val path: String = name.replace('.', '/') + ".class"
        val bytes: ByteArray =
            classes[path]
            ?: throw ClassNotFoundException("Class $name not found in JAR")

        return defineClass(name, bytes, 0, bytes.size)
    }

    companion object {
        private fun loadClassesFromJar(jarBytes: ByteArray): MutableMap<String, ByteArray> {
            val classes: MutableMap<String, ByteArray> = mutableMapOf()
            JarInputStream(ByteArrayInputStream(jarBytes)).use { jarInputStream ->
                while (true) {
                    val entry: JarEntry = jarInputStream.nextJarEntry ?: break
                    if (entry.isDirectory || !entry.name.endsWith(".class")) {
                        continue
                    }

                    classes[entry.name] = jarInputStream.readBytes()
                }
            }
            return classes
        }
    }
}

