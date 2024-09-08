package dev.toastbits.kotules.extension.type.input

import dev.toastbits.kotules.extension.type.ValueType

actual external class ListValue<T: ValueType?>: ValueType {
    @JsName("value")
    val arrayValue: JsArray<T>
}

actual fun <T: ValueType?> ListValue<T>.getListValue(): List<T> {
    if (arrayValue.length == 0 && arrayValue.toString().let { it.isNotBlank() && it != "[]" }) {
        throw RuntimeException("JsArray value within ListValue is not empty but has an imported length of zero ($arrayValue)")
    }
    return List(arrayValue.length) { arrayValue[it] as T }
}
