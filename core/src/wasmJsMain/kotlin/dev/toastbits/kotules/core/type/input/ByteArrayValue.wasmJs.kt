package dev.toastbits.kotules.core.type.input

import dev.toastbits.kotules.core.type.ValueType
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

@Suppress("EXPECTED_EXTERNAL_DECLARATION")
actual external class ByteArrayValue: ValueType {
    val value: Int8Array
}

private fun createByteArrayValue(bytes: Int8Array): ByteArrayValue =
    js("({ value: bytes })")

actual fun ByteArrayValue(value: ByteArray): ByteArrayValue {
    val array: Int8Array = Int8Array(value.size)
    for (i in 0 until value.size) {
        array[i] = value[i]
    }
    return createByteArrayValue(array)
}

actual fun ByteArrayValue.toByteArray(): ByteArray {
    val array: ByteArray = ByteArray(value.length)
    for (i in 0 until value.length) {
        array[i] = value[i]
    }
    return array
}
