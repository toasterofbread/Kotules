package dev.toastbits.kotules.sample.app

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

actual suspend fun loadSampleKotule(): SampleKotule {
    val extensionFileUrl: String = getUrl() + "/" + SampleConfig.JS_EXTENSION_FILE
    println("Loading extension file at $extensionFileUrl")

    val response: HttpResponse = HttpClient().get(extensionFileUrl)
    check(response.status.isSuccess()) { "${response.status} | ${response.bodyAsText()}" }

    println("Loading SampleKotule in JS code at '${SampleConfig.EXTENSION_IMPL_CLASS}'")

    return SampleKotule::class.getLoader().loadFromKotlinJsCode(response.bodyAsText(), SampleConfig.EXTENSION_IMPL_CLASS)
}
