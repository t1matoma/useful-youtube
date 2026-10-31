package dev.satoshi.usefulyoutube.core.network

internal object Fixtures {
    fun read(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)) { "Missing fixture $name" }.readText()
}
