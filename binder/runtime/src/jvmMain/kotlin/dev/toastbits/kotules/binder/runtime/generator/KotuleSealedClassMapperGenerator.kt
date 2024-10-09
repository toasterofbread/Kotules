package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ksp.toClassName
import dev.toastbits.kotules.binder.core.generator.FileGenerator
import dev.toastbits.kotules.binder.core.util.KotuleCoreBinderConstants
import dev.toastbits.kotules.binder.runtime.util.KmpTarget

class KotuleSealedClassMapperGenerator(
    private val scope: FileGenerator.Scope
) {
    private val instanceName: String = KotuleCoreBinderConstants.MAPPER_INSTANCE_NAME

    fun generate(
        name: String,
        kotuleInterface: KSClassDeclaration
    ): FunSpec? =
        if (scope.target == KmpTarget.COMMON || scope.target == KmpTarget.JVM) null
        else FunSpec.builder(name).apply {
            returns(kotuleInterface.toClassName())
            addModifiers(KModifier.INTERNAL)

            val inputClassName: ClassName =
                scope.importFromPackage(
                    KotuleCoreBinderConstants.getInputBindingName(
                        kotuleInterface
                    )
                )
            addParameter(instanceName, inputClassName)

            addCode("return TODO(\"1\")")
        }
        .build()
}
