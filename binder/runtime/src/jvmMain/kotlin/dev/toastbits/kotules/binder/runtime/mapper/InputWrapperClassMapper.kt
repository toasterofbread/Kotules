package dev.toastbits.kotules.binder.runtime.mapper

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.extension.type.StringValue
import kotlin.reflect.KClass

internal fun KSType.getInputWrapperClass(): KClass<*> =
    when (this.toClassName().toString()) {
        "kotlin.String" -> StringValue::class
        else -> throw NotImplementedError(this.toString())
    }
