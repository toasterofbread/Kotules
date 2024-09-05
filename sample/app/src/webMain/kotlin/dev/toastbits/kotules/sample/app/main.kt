@file:OptIn(DelicateCoroutinesApi::class)

package dev.toastbits.kotules.sample.app

import dev.toastbits.kotules.runtime.KotuleLoader
import dev.toastbits.kotules.sample.extension.SampleKotule
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private const val EXTENSION_FILE: String = "extension.js"
private const val EXTENSION_IMPL_CLASS: String = "dev.toastbits.kotules.sample.extension.SampleKotuleImpl"

internal expect fun getUrl(): String

fun main() {
    GlobalScope.launch {
        val extensionFileUrl: String = getUrl() + "/" + EXTENSION_FILE
        println("Loading extension file at $extensionFileUrl")

        val jsCode: String = HttpClient().get(extensionFileUrl).bodyAsText()

        println("Loading SampleKotule in JS code at '$EXTENSION_IMPL_CLASS'")
        val kotule: SampleKotule = KotuleLoader.loadKotlinJs(jsCode, EXTENSION_IMPL_CLASS)

        println("Loaded SampleKotule: $kotule")
        println("Running test() on SampleKotule")
        println(kotule.test())
    }
}
