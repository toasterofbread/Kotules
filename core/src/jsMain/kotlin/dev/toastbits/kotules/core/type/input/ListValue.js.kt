@file:OptIn(ExperimentalJsCollectionsApi::class, ExperimentalJsExport::class)

package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType
import kotlin.js.collections.JsArray
import kotlin.js.collections.JsReadonlyArray
import kotlin.js.collections.toList

actual external class ListValue<T: ValueType?>: ValueType {
    @JsName("value")
    val arrayValue: JsArray<T>
}

private fun <T: ValueType?> initListValue(items: JsReadonlyArray<T>): ListValue<T> =
    js("({ arrayValue: items })") as ListValue<T>

actual fun <T : ValueType?> createListValue(items: List<T>): ListValue<T> =
    initListValue(
        items.asJsReadonlyArrayView()
    )

actual fun <T: ValueType?> ListValue<T>.getListValue(): List<T> {
    println("getListValue 1")
    println("getListValue 2 $this")
    println("getListValue 3 ${this::class}")
    println("getListValue 4 $arrayValue")
    println("getListValue 5 ${arrayValue::class}")
    println("getListValue 6 ${arrayValue.toList()}")
    return arrayValue.toList()
}
