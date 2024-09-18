package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleRuntimeBinderConstants {
    fun getLoaderName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.qualifiedName!!.asString().replace('.', '_') + "_Loader"

    fun getInputBindingGetterName(kotuleInterface: KSClassDeclaration): String =
        "value"

    const val LOADER_INSTANCE_EXTENSION_FUNCTION_NAME: String = "getLoader"
    const val ENUM_BINDER_ORDINAL_PROPERTY_NAME: String = "ordinal"
}
