package dev.toastbits.kotules.binder.extension.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleExtensionBinderConstants {
    fun getOutputBindingName(className: String): String =
        "${className.replace('.', '_')}_OutputBinding"

    fun getOutputBindingName(kotuleClass: KSClassDeclaration): String =
        getOutputBindingName(kotuleClass.simpleName.asString())

    fun getInputMapperName(className: String): String =
        "${className.replace('.', '_')}_InputMapper"

    fun getInputBindingName(className: String): String =
        "${className.replace('.', '_')}_InputBinding"

    const val OUTPUT_BINDING_INSTANCE_NAME: String = "_instance"
}
