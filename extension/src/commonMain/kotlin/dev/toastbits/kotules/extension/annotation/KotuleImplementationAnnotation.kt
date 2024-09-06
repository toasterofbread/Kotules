package dev.toastbits.kotules.extension.annotation

import dev.toastbits.kotules.extension.Kotule
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class KotuleImplementationAnnotation(val bindInterface: KClass<out Kotule>)
