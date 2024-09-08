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
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants

internal fun KSType.getBuiltInInputWrapperClass(scope: FileGenerator.Scope): TypeName? =
    when (this.toClassName().toString()) {
        "kotlin.Int" -> getValueClassName("IntValue")
        "kotlin.String" -> getValueClassName("StringValue")
        "kotlin.collections.List" ->
            getValueClassName("ListValue")
                .parameterizedBy(arguments.map { arg ->
                    val type: KSType = arg.type!!.resolve()
                    type.getBuiltInInputWrapperClass(scope)?.also { return@map it }

                    val declaration: KSDeclaration = type.declaration
                    check(declaration is KSClassDeclaration) { declaration }

                    val typeClass: ClassName = scope.importFromPackage(KotuleRuntimeBinderConstants.getInputBindingName(type.toClassName().simpleName))
                    scope.generateNew(typeClass) {
                        interfaceGenerator.generate(typeClass.simpleName, declaration)?.also {
                            file.addType(it)
                        }
                    }

                    return@map typeClass
                })

        else -> null
    }

private fun getValueClassName(name: String): ClassName =
    ClassName.bestGuess("dev.toastbits.kotules.extension.type.input.$name")
