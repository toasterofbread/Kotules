package dev.toastbits.kotules.sample.app

object SampleConfig {
    const val EXTENSION_IMPL_CLASS: String = "dev.toastbits.kotules.sample.extension.SampleKotuleImpl_OutputBinding"

    const val JS_EXTENSION_FILE: String = "extension.js" // Within server
    const val JVM_EXTENSION_FILE: String = "sample/extension/build/libs/extension-jvm.jar" // Within project root
}
