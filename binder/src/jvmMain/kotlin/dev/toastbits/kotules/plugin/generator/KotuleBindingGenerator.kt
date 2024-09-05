package dev.toastbits.kotules.plugin.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.TypeSpec
import dev.toastbits.kotules.plugin.util.KmpTarget

internal interface KotuleBindingGenerator {
    fun generate(
        className: String,
        kotuleClass: KSClassDeclaration,
        target: KmpTarget
    ): TypeSpec?
}
