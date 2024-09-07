package dev.toastbits.kotules.sample.app

suspend fun runSampleKotule() {
    val kotule: SampleKotule = loadSampleKotule()
    println("Loaded SampleKotule: $kotule")

    println("Calling repeatInput(\"Hello \", 5) on SampleKotule")
    val repeatResult: String = kotule.repeatInput("Hello", 5)
    println("Got result from repeatInput: $repeatResult")

    println("Getting coolProperty from SampleKotule")
    val coolProperty: Int = kotule.coolProperty
    println("Got coolProperty: $coolProperty")

    println("Calling suspendInt() on SampleKotule")
    val intResult: Int = kotule.suspendInt()
    println("Got result from downloadFortune: $intResult")

    println("Calling getDataClass() on SampleKotule")
    val dataClassResult: SampleDataClass = kotule.getDataClass()
    println("Got result from getDataClass: $dataClassResult")

    println("Calling downloadFortune() on SampleKotule")
    val fortuneResult: String = kotule.downloadFortune()
    println("Got result from downloadFortune: $fortuneResult")
}
