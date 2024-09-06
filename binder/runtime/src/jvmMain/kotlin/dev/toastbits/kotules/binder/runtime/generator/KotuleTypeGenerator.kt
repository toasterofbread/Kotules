package dev.toastbits.kotules.binder.runtime.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.TypeSpec
import dev.toastbits.kotules.binder.runtime.util.KmpTarget

internal interface KotuleTypeGenerator {
    fun generate(
        kotuleInterface: KSClassDeclaration,
        target: KmpTarget,
        packageName: String
    ): TypeSpec?
}
