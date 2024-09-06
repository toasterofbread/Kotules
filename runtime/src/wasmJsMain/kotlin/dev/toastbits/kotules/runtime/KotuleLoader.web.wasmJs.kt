package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.extension.type.ValueType

internal actual fun <T: ValueType> new(constructor: JsAny): T = js("new constructor()")

internal actual fun getExtension(): JsAny = js("extension")

private val getObjectEntries: (JsAny) -> JsArray<JsArray<ValueType?>> = js("Object.entries")

internal actual fun entriesOf(jsObject: JsAny): List<Pair<String, JsAny?>> =
    getObjectEntries(jsObject)
        .map { entry -> (entry[0] as JsString).toString() to entry[1] }

@Suppress("UNCHECKED_CAST")
private fun <T: ValueType?, O> JsArray<T>.map(mapper: (T) -> O): List<O> =
    List(length) { i ->
        mapper(this[i] as T)
    }
