package dev.toastbits.kotules.binder.runtime.mapper

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.extension.type.input.IntValue
import dev.toastbits.kotules.extension.type.input.StringValue
import kotlin.reflect.KClass

internal fun KSType.getBuiltInInputWrapperClass(): KClass<*>? =
    when (this.toClassName().toString()) {
        "kotlin.Int" -> IntValue::class
        "kotlin.String" -> StringValue::class
        else -> null
    }
