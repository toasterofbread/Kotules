package dev.toastbits.kotules.sample.app

internal actual fun getUrl(): String = js("window.location.href")
