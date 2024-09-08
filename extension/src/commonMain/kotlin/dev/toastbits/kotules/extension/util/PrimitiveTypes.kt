@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package dev.toastbits.kotules.extension.util

import kotlin.reflect.KClass

val PRIMITIVE_TYPE_CLASSES: List<KClass<*>> =
    listOf(
        String::class,
        Int::class,
        List::class,
        EmptyList::class,
        ArrayList::class
    )

val LIST_TYPES: List<String> =
    listOf(
        "kotlin.collections.List",
        "kotlin.collections.ArrayList",
        "kotlin.collections.EmptyList"
    )

val PRIMITIVE_TYPES: List<String> =
    listOf(
        "kotlin.String",
        "kotlin.Int"
    ) + LIST_TYPES
