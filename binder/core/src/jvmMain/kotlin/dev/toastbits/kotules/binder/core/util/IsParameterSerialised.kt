package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import dev.toastbits.kotules.binder.core.generator.FileGenerator
import dev.toastbits.kotules.core.util.BuiltInType
import dev.toastbits.kotules.extension.util.kotulesJsonInstance
import kotlinx.serialization.Serializable

fun KSDeclaration.getTypeWrapper(): TypeWrapper? {
    if (annotations.any { it.shortName.getShortName() == Serializable::class.simpleName }) {
        return SerializableTypeWrapper
    }

    when (BuiltInType.entries.firstOrNull { it.qualifiedName == qualifiedName!!.asString() }) {
        BuiltInType.ByteArray -> return ByteArrayTypeWrapper
        BuiltInType.IntRange -> return TODOTypeWrapper // TODO
        BuiltInType.CharRange -> return TODOTypeWrapper // TODO
        BuiltInType.LongRange -> return TODOTypeWrapper // TODO
        BuiltInType.UIntRange -> return TODOTypeWrapper // TODO
        BuiltInType.ULongRange -> return TODOTypeWrapper // TODO
        null -> {}
    }

    return null
}

interface TypeWrapper {
    val wrapperType: ClassName
    fun wrap(scope: FileGenerator.Scope): String
    fun unwrap(scope: FileGenerator.Scope): String
}

object TODOTypeWrapper: TypeWrapper {
    override val wrapperType: ClassName = ClassName("dev.toastbits.kotules.core.type.input", "ByteArrayValue")

    override fun wrap(scope: FileGenerator.Scope): String {
        return ".let { TODO() }"
    }

    override fun unwrap(scope: FileGenerator.Scope): String {
        return ".let { TODO() }"
    }
}

object ByteArrayTypeWrapper: TypeWrapper {
    override val wrapperType: ClassName = ClassName("dev.toastbits.kotules.core.type.input", "ByteArrayValue")

    override fun wrap(scope: FileGenerator.Scope): String {
        scope.import(wrapperType)
        return ".let { ${wrapperType.simpleName}(it) }"
    }

    override fun unwrap(scope: FileGenerator.Scope): String {
        val toByteArray: String = "toByteArray"
        scope.import(ClassName(wrapperType.packageName, toByteArray))
        return ".$toByteArray()"
    }
}

object SerializableTypeWrapper: TypeWrapper {
    override val wrapperType: ClassName = String::class.asClassName()

    override fun wrap(scope: FileGenerator.Scope): String {
        scope.import("kotlinx.serialization", "encodeToString")
        return ".let { ${scope.getJson()}.encodeToString(it) }"
    }

    override fun unwrap(scope: FileGenerator.Scope): String {
        return ".let { ${scope.getJson()}.decodeFromString(it) }"
    }

    private fun FileGenerator.Scope.getJson(): String {
        val kotulesJsonInstance: String = (::kotulesJsonInstance).name
        import("dev.toastbits.kotules.extension.util", kotulesJsonInstance)
        return kotulesJsonInstance
    }
}
