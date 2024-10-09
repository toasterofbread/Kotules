package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import dev.toastbits.kotules.binder.core.generator.FileGenerator
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.core.util.getAllDeclarations
import dev.toastbits.kotules.binder.core.util.getListType
import dev.toastbits.kotules.binder.core.util.isPrimitiveType
import dev.toastbits.kotules.binder.runtime.util.KotuleRuntimeBinderConstants
import dev.toastbits.kotules.core.util.ListType

class KotuleBindingInterfaceValueGetterGenerator(
    private val scope: FileGenerator.Scope
) {
    fun generateGetter(
        interfaceType: KSType,
    ): String {
        val kotuleInterface: KSClassDeclaration = interfaceType.declaration as KSClassDeclaration
        val arguments: TypeArgumentInfo = TypeArgumentInfo.from(interfaceType.arguments, kotuleInterface.typeParameters)
        val getterName: String = KotuleRuntimeBinderConstants.getInputBindingGetterName(kotuleInterface)

        check(kotuleInterface.getAllDeclarations().none { it.asType(emptyList()).isPrimitiveType() }) {
            "$interfaceType $kotuleInterface"
        }

        scope.generateNew(
            ClassName(
                scope.file.packageName,
                "${kotuleInterface.qualifiedName!!.asString().replace('.', '_')}_$getterName"
            )
        ) {
            this@generateNew.file.addProperty(
                PropertySpec.Companion.builder(
                    getterName,
                    try {
                        kotuleInterface.toClassName()
                            .run {
                                if (kotuleInterface.typeParameters.isNotEmpty())
                                    parameterizedBy(kotuleInterface.typeParameters.map {
                                        arguments.find(
                                            it
                                        ).toTypeName()
                                    })
                                else this
                            }
                    } catch (e: Throwable) {
                        throw RuntimeException(
                            "$kotuleInterface ${kotuleInterface.typeParameters} $arguments",
                            e
                        )
                    }
                )
                    .addModifiers(KModifier.INTERNAL)
                    .receiver(scope.importFromPackage(KotuleCoreBinderConstants.getInputBindingName(kotuleInterface)))
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode(buildString {
                                append("return ")

                                val interfaceAccessor: String =
                                    kotuleInterface.toClassName().run { canonicalName.removePrefix(packageName + ".") }

                                if (kotuleInterface.isAbstract()) {
                                    append(scope.getMapper(kotuleInterface, arguments).simpleName)
                                    append("(this)")
                                }
                                else if (kotuleInterface.classKind == ClassKind.ENUM_CLASS) {
                                    append(interfaceAccessor)
                                    append(".entries[${KotuleRuntimeBinderConstants.ENUM_BINDER_ORDINAL_PROPERTY_NAME}]")
                                }
                                else {
                                    append(interfaceAccessor)
                                    append('(')
                                    val params: List<KSValueParameter> = kotuleInterface.primaryConstructor?.parameters.orEmpty()
                                    for ((index, param) in params.withIndex()) {
                                        val type: KSType = param.type.resolve()
                                        val actualType: KSType =
                                            (type.declaration as? KSTypeParameter)?.let {
                                                arguments.findOrNull(it)?.resolve()
                                            } ?: type

                                        append(param.name!!.asString())
                                        if (!actualType.isPrimitiveType()) {
                                            if (type.isMarkedNullable) {
                                                append('?')
                                            }

                                            append(".${generateGetter(actualType)}")
                                        }
                                        else {
                                            val listType: ListType? = actualType.getListType()
                                            if (listType != null) {
                                                import("dev.toastbits.kotules.core.type.input", "getListValue")
                                                if (type.isMarkedNullable) {
                                                    append('?')
                                                }

                                                append(".getListValue().map { it")

                                                val argType = actualType.arguments.single().type!!.resolve()
                                                if (argType.isPrimitiveType()) {
                                                    append(".value")
                                                }
                                                else {
                                                    append(".${generateGetter(actualType.arguments.single().type!!.resolve())}")
                                                }

                                                append(" }")

                                                when (listType) {
                                                    ListType.LIST -> {}
                                                    ListType.ARRAY -> append(".toTypedArray()")
                                                    ListType.SEQUENCE -> append(".asSequence()")
                                                    ListType.SET -> append(".toSet()")
                                                }
                                            }
                                        }

                                        if (index + 1 != params.size) {
                                            append(", ")
                                        }
                                    }
                                    append(')')
                                }
                            })
                            .build()
                    )
                    .build()
            )
        }

        return getterName
    }
}
