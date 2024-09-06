package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleRuntimeBinderConstants {
    fun getInputBindingName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.simpleName.asString() + "_InputBinding"

    fun getMapperName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.simpleName.asString() + "_Mapper"

    fun getLoaderName(kotuleInterface: KSClassDeclaration): String =
        kotuleInterface.simpleName.asString() + "_Loader"

    const val MAPPER_INSTANCE_NAME: String = "_instance"
}
