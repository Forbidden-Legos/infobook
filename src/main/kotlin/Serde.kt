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
    Go to the next page to see all different types of markdown we support
    [Page 3](page:3)

    *italic*
    **bold**
    __underline__
    ~~strikethrough~~
    ||obfuscated||
    [link](https://google.com/)
    [page](page:1)
    [book](book:root)
    [command](comm:kill @s)
    <color ff0000>color</color>
    <c 00ff00>color but short</c>
    <formatting 3l>formatting</formatting>
    <f 4>formatting but short</f>
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
