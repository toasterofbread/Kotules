@file:OptIn(ExperimentalJsCollectionsApi::class)

package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType
import dev.toastbits.kotules.core.type.input.ListValue
import kotlin.js.collections.JsArray
import kotlin.js.collections.toList

actual external class ListValue<T: ValueType?>: ValueType {
    @JsName("value")
    val arrayValue: JsArray<T>
}

actual fun <T: ValueType?> ListValue<T>.getListValue(): List<T> =
    arrayValue.toList()
