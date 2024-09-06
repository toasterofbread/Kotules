package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.Kotule
import dev.toastbits.kotules.extension.type.ValueType

private external fun eval(code: String)

actual interface KotuleLoader<T: Kotule> {
    fun loadFromKotlinJsCode(jsCode: String, implementationClass: String): T
}

fun <T: KotuleInputBinding> loadKotuleInputBindingFromKotlinJsCode(jsCode: String, implementationClass: String): T {
    eval(jsCode)

    val extension: ValueType = getExtension()
    val moduleConstructor: ValueType = extension.dot(implementationClass)
    val moduleInstance: T = new(moduleConstructor)

    return moduleInstance
}

internal expect fun <T: ValueType> new(constructor: ValueType): T

internal expect fun getExtension(): ValueType

internal expect fun entriesOf(jsObject: ValueType): List<Pair<String, ValueType?>>

private fun ValueType.dot(path: String): ValueType {
    var currentObj: ValueType = this
    for (part in path.split('.')) {
        val entries: List<Pair<String, ValueType?>> = entriesOf(currentObj)
        try {
            currentObj = entries.first { it.first == part }.second!!
        }
        catch (e: Throwable) {
            throw RuntimeException("Could not find entry '$part' in $entries ($path)", e)
        }
    }
    return currentObj
}
