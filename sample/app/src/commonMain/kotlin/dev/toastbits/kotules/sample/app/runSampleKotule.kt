package dev.toastbits.kotules.sample.app

suspend fun runSampleKotule() {
    val kotule: SampleKotule = loadSampleKotule()
    println("Loaded SampleKotule: $kotule")

//    println("Calling inputTest() on SampleKotule")
//    val input: SampleInputInterface =
//        object : SampleInputInterface {
//            override fun getText(): String = "Hello"
//        }
//    val inputResult: String = kotule.inputTest(input)
//    println("Got result from inputTest: $inputResult")
//    TODO()

    println("Calling repeatInput(\"Hello \", 5) on SampleKotule")
    val repeatResult: String = kotule.repeatInput("Hello", 5)
    println("Got result from repeatInput: $repeatResult")

    println("Getting intProperty from SampleKotule")
    val intProperty: Int = kotule.intProperty
    println("Got intProperty: $intProperty")

    println("Calling suspendInt() on SampleKotule")
    val intResult: Int = kotule.suspendInt()
    println("Got result from downloadFortune: $intResult")

    println("Calling getDataClass() on SampleKotule")
    val dataClassResult: SampleDataClass = kotule.getDataClass()
    println("Got result from getDataClass: $dataClassResult")

    println("Calling getList() on SampleKotule")
    val listResult: List<SampleDataClass> = kotule.getList()
    println("Got result from getList: $listResult")

    println("Calling downloadFortune() on SampleKotule")
    val fortuneResult: String = kotule.downloadFortune()
    println("Got result from downloadFortune: $fortuneResult")
}
