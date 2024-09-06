package dev.toastbits.kotules.binder.extension.mapper

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.extension.type.OutStringValue
import dev.toastbits.kotules.extension.type.StringValue
import kotlin.reflect.KClass

internal fun KSType.getOutputWrapperClass(): KClass<*> =
    when (this.toClassName().toString()) {
        "kotlin.String" -> OutStringValue::class
        else -> throw NotImplementedError(this.toString())
    }
