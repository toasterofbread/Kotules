package dev.toastbits.kotules.binder.runtime.util

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier

fun Sequence<KSFunctionDeclaration>.filterRelevantFunctions(kotuleClass: KSClassDeclaration): Sequence<KSFunctionDeclaration> =
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
