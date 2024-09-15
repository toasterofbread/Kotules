package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleRuntimeBinderConstants {
    fun getLoaderName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.simpleName.asString() + "_Loader"

    const val LOADER_INSTANCE_EXTENSION_FUNCTION_NAME: String = "getLoader"
}
