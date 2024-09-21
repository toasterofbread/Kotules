@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package dev.toastbits.kotules.core.util

import kotlin.reflect.KClass

fun KClass<*>.isPrimitive(): Boolean =
    PRIMITIVE_TYPE_CLASSES.none { this.isInstance(it) }

private val PRIMITIVE_TYPE_CLASSES: List<KClass<*>> =
    listOf(
        Unit::class,
        Boolean::class,
        Int::class,
        UInt::class,
        Byte::class,
        UByte::class,
        Short::class,
        UShort::class,
        Long::class,
        ULong::class,
        String::class,
        List::class,
        EmptyList::class,
        ArrayList::class,
        Set::class,
        EmptySet::class,
        Array::class,
        Sequence::class,
        Function::class
    )

val LIST_TYPES: Map<String, ListType> =
    mapOf(
        "kotlin.collections.List" to ListType.LIST,
        "kotlin.collections.ArrayList" to ListType.LIST,
        "kotlin.collections.EmptyList" to ListType.LIST,
        "kotlin.collections.Set" to ListType.SET,
        "kotlin.collections.EmptySet" to ListType.SET,
        "kotlin.Array" to ListType.ARRAY,
        "kotlin.sequences.Sequence" to ListType.SEQUENCE
    )

enum class ListType {
    LIST,
    ARRAY,
    SEQUENCE,
    SET
}

enum class BuiltInType {
    ByteArray {
        override val qualifiedName = "kotlin.ByteArray"
    },
    IntRange {
        override val qualifiedName = "kotlin.ranges.IntRange"
    },
    CharRange {
        override val qualifiedName = "kotlin.ranges.CharRange"
    },
    LongRange {
        override val qualifiedName = "kotlin.ranges.LongRange"
    },
    UIntRange {
        override val qualifiedName = "kotlin.ranges.UIntRange"
    },
    ULongRange {
        override val qualifiedName = "kotlin.ranges.ULongRange"
    };

    abstract val qualifiedName: String
}

val PRIMITIVE_TYPES: List<String> =
    listOf(
        "kotlin.Unit",
        "kotlin.Boolean",
        "kotlin.Int",
        "kotlin.UInt",
        "kotlin.Byte",
        "kotlin.UByte",
        "kotlin.Short",
        "kotlin.UShort",
        "kotlin.Long",
        "kotlin.ULong",
        "kotlin.String"
    ) + LIST_TYPES.keys + listOf("kotlin.Function") + (1..20).map { "kotlin.Function$it" }
