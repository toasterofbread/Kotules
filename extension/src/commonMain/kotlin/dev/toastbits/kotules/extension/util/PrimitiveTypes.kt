@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package dev.toastbits.kotules.extension.util

import kotlin.reflect.KClass

val PRIMITIVE_TYPE_CLASSES: List<KClass<*>> =
    listOf(
        Boolean::class,
        Int::class,
        String::class,
        List::class,
        EmptyList::class,
        ArrayList::class,
        Set::class,
        EmptySet::class,
        Function::class,
    )

val LIST_TYPES: List<String> =
    listOf(
        "kotlin.collections.List",
        "kotlin.collections.ArrayList",
        "kotlin.collections.EmptyList",
        "kotlin.collections.Set",
        "kotlin.collections.EmptySet"
    )

val PRIMITIVE_TYPES: List<String> =
    listOf(
        "kotlin.Boolean",
        "kotlin.Int",
        "kotlin.String",
    ) + LIST_TYPES
