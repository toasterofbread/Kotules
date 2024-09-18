package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.symbol.KSClassDeclaration

object KotuleCoreBinderConstants {
    const val MAPPER_INSTANCE_NAME: String = "_instance"

    fun getInputMapperName(interfaceName: String): String =
        "${interfaceName.replace('.', '_')}_CoreInputMapper"

    fun getInputMapperName(kotuleInterface: KSClassDeclaration): String =
        getInputMapperName(kotuleInterface.qualifiedName!!.asString())

    fun getInputBindingName(interfaceName: String): String =
        "${interfaceName.replace('.', '_')}_CoreInputBinding"

    fun getInputBindingName(kotuleInterface: KSClassDeclaration): String =
        getInputBindingName(kotuleInterface.qualifiedName!!.asString())
}