package de.olivermakesco.infobook

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.quiltmc.loader.api.QuiltLoader
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@JvmRecord
@Serializable
data class Config(
    val defaultPath: String,
    val openOnFirstJoin: Boolean,
    val playerCache: ArrayList<String>
)

val defaultText = """
    This is page 1
    <c ff0000>red!</c>
    [Page 2](page:2)

    This is page 2
    ||obfuscated||
    Two line breaks signify a new page
""".trimIndent()

var config: Config = Config("root", false, arrayListOf())

val path = QuiltLoader.getConfigDir().resolve("infobook.json")

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = true
}

fun loadConfig() {
    if (!path.exists()) {
        config = Config("root", false, arrayListOf())
        path.createFile()
        val root = getPath()
        if (!root.exists()) {
            root.createFile()
            root.writeText(defaultText)
        }
        return
    }
    config = json.decodeFromString(path.readText())
    val root = getPath()
    if (!root.exists()) {
        root.createFile()
        root.writeText(defaultText)
    }
}

fun saveConfig() {
    if (!path.exists()) {
        path.createFile()
    }
    path.writeText(json.encodeToString(config))
}
