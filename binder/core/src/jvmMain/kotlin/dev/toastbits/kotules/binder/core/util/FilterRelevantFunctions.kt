package dev.toastbits.kotules.binder.core.util

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier

fun KSClassDeclaration.getAbstractFunctions(): Sequence<KSFunctionDeclaration> =
    getAllDeclarations().flatMap { it.getDeclaredFunctions() }.distinctBy { it.toString() }.asSequence().filterRelevantFunctions(this)

fun KSClassDeclaration.getAbstractProperties(): Sequence<KSPropertyDeclaration> =
    getAllDeclarations().flatMap { it.getDeclaredProperties() }.distinctBy { it.simpleName.asString() }.asSequence()

fun KSClassDeclaration.getAllDeclarations(): List<KSClassDeclaration> =
    listOf(this) + this.getAllSuperTypes().map { it.declaration }.filterIsInstance<KSClassDeclaration>()

private fun Sequence<KSFunctionDeclaration>.filterRelevantFunctions(kotuleClass: KSClassDeclaration): Sequence<KSFunctionDeclaration> =
    filter { function ->
        val functionName: String = function.simpleName.asString()
        if (Any::class.java.declaredMethods.any { it.name == functionName }) {
            return@filter false
        }

        if (kotuleClass.modifiers.contains(Modifier.DATA)) {
            if (functionName == "copy") {
                return@filter false
            }

            if (
                (1 .. kotuleClass.primaryConstructor!!.parameters.size)
                    .any { functionName == "component$it" }
            ) {
                return@filter false
            }
        }

        return@filter true
    }
