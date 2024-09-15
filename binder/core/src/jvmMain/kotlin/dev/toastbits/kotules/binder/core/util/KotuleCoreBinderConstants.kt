package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleCoreBinderConstants {
    const val MAPPER_INSTANCE_NAME: String = "_instance"

    fun getInputMapperName(interfaceName: String): String =
        "${interfaceName}_InputMapper"

    fun getInputMapperName(kotuleInterface: KSClassDeclaration): String =
        getInputMapperName(kotuleInterface.simpleName.asString())

    fun getInputBindingName(interfaceName: String): String =
        "${interfaceName}_InputBinding"

    fun getInputBindingName(kotuleInterface: KSClassDeclaration): String =
        getInputBindingName(kotuleInterface.simpleName.asString())
}