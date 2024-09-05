package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.Kotule
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.reflect.full.isSuperclassOf

actual object KotuleLoader {
    inline fun <reified T> loadFromJar(path: String, implementationClass: String): T {
        require(!Kotule::class.isSuperclassOf(T::class))

        val jarPath: Path = Path(System.getProperty("user.dir")).resolve("../../").resolve(path)

        val classLoader: URLClassLoader =
            URLClassLoader(
                arrayOf(URL("file://$jarPath")),
                this.javaClass.classLoader
            )

        val cls: Class<*> = Class.forName(implementationClass, true, classLoader)
        return cls.getConstructor().newInstance() as T
    }
}
