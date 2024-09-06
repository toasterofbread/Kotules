package dev.toastbits.kotules.binder.extension.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeSpec
import dev.toastbits.kotules.binder.extension.util.KmpTarget

internal interface KotuleTypeGenerator {
    fun generate(
        className: String,
        kotuleClass: KSClassDeclaration,
        bindInterface: KSType,
        target: KmpTarget
    ): TypeSpec?
}
