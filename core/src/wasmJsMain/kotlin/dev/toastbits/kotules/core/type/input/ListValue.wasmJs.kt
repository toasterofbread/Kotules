package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType

actual external class ListValue<T: ValueType?>: ValueType {
    @JsName("value")
    val arrayValue: JsArray<T>
}

external fun <T: ValueType?> JsArray(size: Int): JsArray<T>

private fun <T: ValueType?> initListValue(items: JsArray<T>): ListValue<T> =
    js("({ arrayValue: items })")

actual fun <T : ValueType?> createListValue(items: List<T>): ListValue<T> =
    initListValue(
        JsArray<T>(items.size).also { array ->
            for (index in 0 until items.size) {
                array[index] = items[index]
            }
        }
    )

actual fun <T: ValueType?> ListValue<T>.getListValue(): List<T> {
    println("getListValue 1")
    println("getListValue 2 $this")
    println("getListValue 3 ${this::class}")
    println("getListValue 4 $arrayValue")
    println("getListValue 5 ${arrayValue::class}")

    if (arrayValue.length == 0 && arrayValue.toString().let { it.isNotBlank() && it != "[]" }) {
        throw RuntimeException("JsArray value within ListValue is not empty but has an imported length of zero ($arrayValue)")
    }
    return List(arrayValue.length) { arrayValue[it] as T }
}
