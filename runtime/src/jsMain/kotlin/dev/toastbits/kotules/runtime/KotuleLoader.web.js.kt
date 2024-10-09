@file:OptIn(ExperimentalJsCollectionsApi::class)

package dev.toastbits.kotules.runtime

import dev.toastbits.kotules.core.type.ValueType
import kotlin.js.collections.JsArray
import kotlin.js.collections.toList

internal actual fun <T: ValueType> new(constructor: ValueType): T = js("new constructor()").unsafeCast<T>()

internal actual fun getOnWindow(key: String): ValueType = js("window[key]").unsafeCast<ValueType>()

private fun getWindowKeysArray(): JsArray<String> = js("Object.keys(window)") as JsArray<String>

internal actual fun getWindowKeys(): List<String> = getWindowKeysArray().toList()

private val getObjectEntries: (dynamic) -> Array<Array<dynamic>> =
    js("Object.entries") as (dynamic) -> Array<Array<dynamic>>

internal actual fun entriesOf(jsObject: dynamic): List<Pair<String, dynamic>> =
    getObjectEntries(jsObject)
        .map { entry -> entry[0] as String to entry[1] }
