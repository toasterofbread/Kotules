package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.JsType

private external fun eval(code: String)

actual object KotuleLoader {
    fun <T: JsType> loadKotlinJs(jsCode: String, implementationClass: String): T {
        eval(jsCode)

        val extension: JsType = getExtension()
        val moduleConstructor: JsType = extension.dot(implementationClass)
        val moduleInstance: T = newKotule(moduleConstructor)

        return moduleInstance
    }
}

internal expect fun <T: JsType> newKotule(cls: JsType): T

internal expect fun getExtension(): JsType

internal expect fun entriesOf(jsObject: JsType): List<Pair<String, JsType?>>

private fun JsType.dot(path: String): JsType {
    var currentObj: JsType = this
    for (part in path.split('.')) {
        val entries: List<Pair<String, JsType?>> = entriesOf(currentObj)
        try {
            currentObj = entries.first { it.first == part }.second!!
        }
        catch (e: Throwable) {
            throw RuntimeException("Could not find entry '$part' in $entries ($path)", e)
        }
    }
    return currentObj
}
