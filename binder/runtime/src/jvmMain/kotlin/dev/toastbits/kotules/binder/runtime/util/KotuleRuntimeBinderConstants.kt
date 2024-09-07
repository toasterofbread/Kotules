package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleRuntimeBinderConstants {
    fun getInputBindingName(interfaceName: String): String =
        "${interfaceName}_InputBinding"

    fun getInputBindingName(kotuleInterface: KSClassDeclaration): String =
        getInputBindingName(kotuleInterface.simpleName.asString())

    fun getMapperName(interfaceName: String): String =
        "${interfaceName}_Mapper"

    fun getMapperName(kotuleInterface: KSClassDeclaration): String =
        getMapperName(kotuleInterface.simpleName.asString())

    fun getLoaderName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.simpleName.asString() + "_Loader"

    const val MAPPER_INSTANCE_NAME: String = "_instance"
    const val LOADER_INSTANCE_EXTENSION_FUNCTION_NAME: String = "getLoader"
}
