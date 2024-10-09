package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias

fun KSType.resolveTypeAlias(): KSType {
    var type: KSType = this
    while (true) {
        val declaration: KSDeclaration = type.declaration
        if (declaration is KSTypeAlias) {
            type = declaration.type.resolve()
        }
        else if (isMarkedNullable) {
            return type.makeNullable()
        }
        else {
            return type
        }
    }
}
