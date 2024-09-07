package dev.toastbits.kotules.binder.extension.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleExtensionBinderConstants {
    fun getOutputBindingName(className: String): String =
        "${className}_OutputBinding"

    fun getOutputBindingName(kotuleClass: KSClassDeclaration): String =
        getOutputBindingName(kotuleClass.simpleName.asString())

    const val OUTPUT_BINDING_INSTANCE_NAME: String = "_instance"
}
