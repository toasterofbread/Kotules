package dev.toastbits.kotules.extension

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE
)
actual annotation class PlatformJsName actual constructor(actual val name: String)
