package dev.toastbits.kotules.plugin.provider

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.toastbits.kotules.plugin.binder.KotuleAnnotationProcessor

class KotuleAnnotationProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        KotuleAnnotationProcessor(environment)
}
