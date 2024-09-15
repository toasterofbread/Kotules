package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias

fun KSType.resolveTypeAlias(): String {
    var declaration: KSDeclaration = declaration
    while (declaration is KSTypeAlias) {
        declaration = declaration.type.resolve().declaration
    }
    return declaration.qualifiedName!!.asString()
}
