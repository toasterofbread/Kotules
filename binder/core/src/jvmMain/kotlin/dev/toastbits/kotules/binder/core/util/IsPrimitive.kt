package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import dev.toastbits.kotules.core.util.LIST_TYPES
import dev.toastbits.kotules.core.util.ListType
import dev.toastbits.kotules.core.util.PRIMITIVE_TYPES

fun KSType.isPrimitiveType(): Boolean =
    (declaration as? KSClassDeclaration)?.getAllDeclarations()?.any {
        val qualifiedName: String = it.qualifiedName!!.asString()
        return@any PRIMITIVE_TYPES.contains(qualifiedName)
    } ?: false

fun KSType.isListType(strict: Boolean = false): Boolean =
    if (strict) getListType() == ListType.LIST
    else getListType() != null

fun KSType.getListType(): ListType? =
    (declaration as? KSClassDeclaration)?.getAllDeclarations()?.firstNotNullOfOrNull {
        return@firstNotNullOfOrNull LIST_TYPES[it.qualifiedName!!.asString()]
    }
