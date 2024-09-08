package dev.toastbits.kotules.binder.runtime.mapper

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants

internal fun KSType.getBuiltInInputWrapperClass(): TypeName? =
    when (this.toClassName().toString()) {
        "kotlin.Int" -> getValueClassName("IntValue")
        "kotlin.String" -> getValueClassName("StringValue")
        "kotlin.collections.List" ->
            getValueClassName("ListValue")
                .parameterizedBy(arguments.map { arg ->
                    val type: KSType = arg.type!!.resolve()
                    return@map (
                        type.getBuiltInInputWrapperClass()
                        ?: ClassName.bestGuess(KotuleRuntimeBinderConstants.getInputBindingName(type.toClassName().simpleName))
                    )
                })

        else -> null
    }

private fun getValueClassName(name: String): ClassName =
    ClassName.bestGuess("dev.toastbits.kotules.extension.type.input.$name")
