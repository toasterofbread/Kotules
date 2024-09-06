package dev.toastbits.kotules.binder.extension.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleExtensionBinderConstants {
    fun getOutputBindingName(kotuleClass: KSClassDeclaration): String =
        kotuleClass.simpleName.asString() + "_OutputBinding"

    const val OUTPUT_BINDING_INSTANCE_NAME: String = "_instance"
}
