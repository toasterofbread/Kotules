package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.symbol.KSDeclaration
import kotlinx.serialization.Serializable

fun KSDeclaration.shouldBeSerialised(): Boolean =
    annotations.any { it.shortName.getShortName() == Serializable::class.simpleName }
