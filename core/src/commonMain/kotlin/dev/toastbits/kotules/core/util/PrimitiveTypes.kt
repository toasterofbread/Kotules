@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package dev.toastbits.kotules.core.util

import kotlin.reflect.KClass

val PRIMITIVE_TYPE_CLASSES: List<KClass<*>> =
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
        Function::class,
    )

val LIST_TYPES: List<String> =
    listOf(
        "kotlin.collections.List",
        "kotlin.collections.ArrayList",
        "kotlin.collections.EmptyList",
        "kotlin.collections.Set",
        "kotlin.collections.EmptySet",
        "kotlin.Array",
        "kotlin.sequences.Sequence"
    )

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
        "kotlin.String",
    ) + LIST_TYPES
