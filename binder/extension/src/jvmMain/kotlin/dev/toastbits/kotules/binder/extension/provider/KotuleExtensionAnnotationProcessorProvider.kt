package dev.toastbits.kotules.binder.extension.provider

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.toastbits.kotules.binder.extension.processor.KotuleExtensionAnnotationProcessor

class KotuleExtensionAnnotationProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        KotuleExtensionAnnotationProcessor(environment)
}
