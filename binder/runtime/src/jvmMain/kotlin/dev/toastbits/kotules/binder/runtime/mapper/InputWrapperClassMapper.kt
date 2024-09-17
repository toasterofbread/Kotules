package dev.toastbits.kotules.binder.runtime.mapper

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.runtime.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.core.util.LIST_TYPES

internal fun KSType.getBuiltInInputWrapperClass(scope: FileGenerator.Scope): TypeName? {
    val className: String =
        try {
            this.toClassName().toString()
        }
        catch (_: Throwable) {
            return null
        }

    when (className) {
        "kotlin.Boolean" -> return getValueClassName("BooleanValue")
        "kotlin.Int" -> return getValueClassName("IntValue")
        "kotlin.String" -> return getValueClassName("StringValue")
    }

    if (LIST_TYPES.contains(className)) {
        return getValueClassName("ListValue")
            .parameterizedBy(arguments.map { arg ->
                val type: KSType = arg.type!!.resolve()
                type.getBuiltInInputWrapperClass(scope)?.also { return@map it }

                val declaration: KSDeclaration = type.declaration
                check(declaration is KSClassDeclaration) { declaration }

                val typeClass: ClassName = scope.importFromPackage(KotuleCoreBinderConstants.getInputBindingName(type.toClassName().canonicalName))
                scope.generateNew(typeClass) {
                    interfaceGenerator.generate(typeClass.simpleName, declaration, true)?.also {
                        file.addType(it)
                    }
                }

                return@map typeClass
            })
    }

    return null
}

private fun getValueClassName(name: String): ClassName =
    ClassName.bestGuess("dev.toastbits.kotules.core.type.input.$name")
