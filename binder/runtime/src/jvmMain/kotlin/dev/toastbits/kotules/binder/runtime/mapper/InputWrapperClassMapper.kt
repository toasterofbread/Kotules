package dev.toastbits.kotules.binder.runtime.mapper

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.core.util.isListType
import dev.toastbits.kotules.binder.core.generator.FileGenerator
import dev.toastbits.kotules.binder.runtime.processor.interfaceGenerator

internal fun KSType.getBuiltInInputWrapperClass(scope: FileGenerator.Scope): TypeName? {
    if (isListType()) {
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
        "kotlin.Enum" -> return getValueClassName("EnumValue")
    }

    return null
}

private fun getValueClassName(name: String): ClassName =
    ClassName.bestGuess("dev.toastbits.kotules.core.type.input.$name")
