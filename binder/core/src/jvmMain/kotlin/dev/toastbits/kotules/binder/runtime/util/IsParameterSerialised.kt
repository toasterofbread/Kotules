package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSDeclaration
import kotlinx.serialization.Serializable

fun KSDeclaration.shouldBeSerialsied(): Boolean =
    annotations.any { it.shortName.getShortName() == Serializable::class.simpleName }
