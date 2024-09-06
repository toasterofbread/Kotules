package dev.toastbits.kotules.extension

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE
)
expect annotation class PlatformJsName(val name: String)
