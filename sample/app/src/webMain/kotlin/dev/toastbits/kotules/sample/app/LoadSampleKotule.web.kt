package dev.toastbits.kotules.sample.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

actual suspend fun loadSampleKotule(): SampleKotule {
    val extensionFileUrl: String = getUrl() + "/" + SampleConfig.JS_EXTENSION_FILE
    println("Loading extension file at $extensionFileUrl")

    val jsCode: String = HttpClient().get(extensionFileUrl).bodyAsText()
    println("Loading SampleKotule in JS code at '${SampleConfig.EXTENSION_IMPL_CLASS}'")

    return SampleKotule_Loader.loadFromKotlinJsCode(jsCode, SampleConfig.EXTENSION_IMPL_CLASS)
}
