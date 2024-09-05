package dev.toastbits.kotules.extension

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FILE
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

@Target(CLASS, PROPERTY, FUNCTION, FILE)
actual annotation class PlatformJsExport
