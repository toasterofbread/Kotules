package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.type.JsType

internal actual fun <T: JsType> newKotule(cls: JsAny): T = js("new cls()")

internal actual fun getExtension(): JsAny = js("extension")

private val getObjectEntries: (JsAny) -> JsArray<JsArray<JsType?>> = js("Object.entries")

internal actual fun entriesOf(jsObject: JsAny): List<Pair<String, JsAny?>> =
    getObjectEntries(jsObject)
        .map { entry -> (entry[0] as JsString).toString() to entry[1] }

@Suppress("UNCHECKED_CAST")
private fun <T: JsType?, O> JsArray<T>.map(mapper: (T) -> O): List<O> =
    List(length) { i ->
        mapper(this[i] as T)
    }
