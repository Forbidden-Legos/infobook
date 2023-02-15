package de.olivermakesco.infobook

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import de.olivermakesco.infobook.markdown.XmlNode
import de.olivermakesco.infobook.markdown.XmlRule
import dev.proxyfox.markt.*
import eu.pb4.sgui.api.elements.BookElementBuilder
import eu.pb4.sgui.api.gui.BookGui
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.*
import net.minecraft.text.component.TextComponent
import net.minecraft.util.Formatting
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.loader.api.QuiltLoader
import org.quiltmc.qkl.library.brigadier.argument.greedyString
import org.quiltmc.qkl.library.brigadier.execute
import org.quiltmc.qkl.library.brigadier.register
import org.quiltmc.qkl.library.brigadier.required
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.command.api.CommandRegistrationCallback
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.*

val parser = MarkdownParser()

@Suppress("UNUSED")
object InfobookMod : ModInitializer {
    override fun onInitialize(mod: ModContainer) {
        loadConfig()
        parser {
            +BracketRule("*")  // Italics
            +BracketRule("**") // Bold
            +BracketRule("__") // Underline
            +BracketRule("~~") // Strikethrough
            +BracketRule("||") // Obfuscated
            +HyperlinkRule
            +XmlRule("color")
            +XmlRule("c")
            +XmlRule("formatting")
            +XmlRule("f")
        }

        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register("info") {
                execute {
                    try {
                        open(source.player, config.defaultPath)
                    } catch (err: Throwable) {
                        err.printStackTrace()
                    }
                }
                required(greedyString("path")) {
                    execute {
                        open(source.player, getArgument("path", String::class.java))
                    }
                    suggests(PathSuggestions)
                }
            }
        }
    }
}

object PathSuggestions : SuggestionProvider<ServerCommandSource> {
    override fun getSuggestions(
        context: CommandContext<ServerCommandSource>,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        val future = CompletableFuture<Suggestions>()
        future.completeAsync {
            val path = QuiltLoader.getConfigDir().resolve("infobook")
            appendPaths(path, "", builder)
            builder.build()
        }
        return future
    }

    private fun appendPaths(path: Path, currPath: String, builder: SuggestionsBuilder) {
        for (sub in path.toFile().list() ?: arrayOf()) {
            val next = path.resolve(sub)
            if (next.isDirectory())
                appendPaths(next, "$currPath$sub/", builder)
            else if (sub.endsWith(".md"))
                builder.suggest("$currPath${sub.substring(0, sub.length-3)}")
        }
    }
}

fun onPlayerJoin(player: ServerPlayerEntity) {
    if (!config.playerCache.contains(player.uuidAsString)) {
        config.playerCache += player.uuidAsString
        if (config.openOnFirstJoin)
            open(player, config.defaultPath)
        saveConfig()
    }
}

fun getPath(path: String = "root"): Path {
    if (path.isEmpty()) return getPath("root")
    return QuiltLoader.getConfigDir().resolve("infobook").resolve("$path.md")
}

fun open(player: ServerPlayerEntity, path: String = "") {
    val bookPath = getPath(path)
    if (!bookPath.exists()) {
        player.sendMessage(Text.of("Info book not found."), false)
    }
    val text = bookPath.readText().split("\n\n")
    player.closeHandledScreen()
    book(player) {
        for (line in text) {
            +line.split("\n").markdownLines
        }
    }.open()
}

fun book(player: ServerPlayerEntity, builder: BookElementBuilder.() -> Unit) = BookGui(player, BookElementBuilder().apply(builder))

context(BookElementBuilder)
operator fun Text.unaryPlus() = addPage(this)
context(BookElementBuilder)
operator fun List<Text>.unaryPlus() = addPage(*this.toTypedArray())

val List<String>.markdownLines: List<Text> get() {
    val out = arrayListOf<Text>()
    for (line in this) out += line.markdownText
    return out
}
val String.markdownText get() = parser.parse(this).text()

fun String.getFormat(): Formatting {
    return when (this) {
        "*" -> Formatting.ITALIC
        "**" -> Formatting.BOLD
        "__" -> Formatting.UNDERLINE
        "~~" -> Formatting.STRIKETHROUGH
        "||" -> Formatting.OBFUSCATED
        else -> Formatting.RESET
    }
}

fun MarkdownNode.text(): Text {
    if (this is RootNode) {
        var text = MutableText.create(TextComponent.EMPTY)
        for (node in nodes) {
            text = text.append(node.text())
        }
        return text
    }
    if (this is StringNode) return Text.of(value)
    if (this is SymbolNode) {
        var text = MutableText.create(TextComponent.EMPTY).formatted(left.getFormat())
        for (node in nodes) {
            text = text.append(node.text())
        }
        return text
    }
    if (this is HyperlinkNode) {
        var style = Style.EMPTY
            .withFormatting(Formatting.DARK_BLUE)
            .withFormatting(Formatting.UNDERLINE)

        style = if (url.startsWith("comm:")) {
            style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${url.substring(5)}"))
        } else if (url.startsWith("page:")) {
            style.withClickEvent(ClickEvent(ClickEvent.Action.CHANGE_PAGE, url.substring(5)))
        } else if (url.startsWith("book:")) {
            style.withClickEvent(ClickEvent(ClickEvent.Action.RUN_COMMAND, "/info ${url.substring(5)}"))
        } else {
            style.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
        }
        var text = MutableText.create(TextComponent.EMPTY).setStyle(style)
        for (node in nodes) {
            text = text.append(node.text())
        }
        return text
    }
    if (this is XmlNode) {
        if (key == "color" || key == "c") {
            parameters ?: return Text.empty()
            val color = parameters.toIntOrNull(16) ?: parameters.substring(1).toIntOrNull(16) ?: return Text.empty()
            var text = MutableText.create(TextComponent.EMPTY).setStyle(Style.EMPTY.withColor(color))
            for (node in nodes) {
                text = text.append(node.text())
            }
            return text
        }
        if (key == "formatting" || key == "f") {
            parameters ?: return Text.empty()
            var style = Style.EMPTY
            for (char in parameters) {
                style = style.withFormatting(Formatting.byCode(char) ?: continue)
            }
            var text = MutableText.create(TextComponent.EMPTY).setStyle(style)
            for (node in nodes) {
                text = text.append(node.text())
            }
            return text
        }
    }

    return Text.of(toString())
}
