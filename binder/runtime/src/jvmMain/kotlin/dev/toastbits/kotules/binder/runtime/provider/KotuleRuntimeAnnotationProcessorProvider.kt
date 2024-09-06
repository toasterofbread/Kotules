package dev.toastbits.kotules.binder.runtime.provider

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import dev.toastbits.kotules.binder.runtime.processor.KotuleRuntimeAnnotationProcessor

class KotuleRuntimeAnnotationProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        KotuleRuntimeAnnotationProcessor(environment)
}
