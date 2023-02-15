package de.olivermakesco.infobook.markdown

import dev.proxyfox.markt.MarkdownNode
import dev.proxyfox.markt.MarkdownParser
import dev.proxyfox.markt.MarkdownRule

class XmlRule(val key: String) : MarkdownRule {
    override val triggerLength: Int = 1

    override fun parse(content: String, parser: MarkdownParser): MarkdownNode? {
        if (content.startsWith("<$key />")) {
            return EmptyXmlNode(key, true)
        } else if (content.startsWith("<$key/>")) {
            return EmptyXmlNode(key)
        } else if (content.startsWith("<$key >")) {
            val idx = parseToClose(content, 3+key.length, parser)
            val sub = content.substring(3+key.length, idx)
            return XmlNode(key, sub, parser, true, null)
        } else if (content.startsWith("<$key>")) {
            val idx = parseToClose(content, 2+key.length, parser)
            val sub = content.substring(2+key.length, idx)
            return XmlNode(key, sub, parser, false, null)
        } else if (content.startsWith("<$key ")) {
            var idx = key.length+2
            var closed = false
            var params = ""
            while (idx < content.length) {
                if (content[idx] == '>') {
                    closed = true
                    break
                }
                params += content[idx]
                idx++
            }
            if (!closed) return null
            if (content[idx-1] == '\\') return EmptyXmlNode(key, true, params.substring(0, params.length-1))
            val old = idx+1
            idx = parseToClose(content, old, parser)
            val sub = content.substring(old, idx)
            return XmlNode(key, sub, parser, true, params)
        }
        return null
    }
    fun parseToClose(content: String, idx: Int, parser: MarkdownParser): Int {
        var i = idx
        while (i < content.length) {
            if (content.substring(i).startsWith("</$key>")) break
            val tryParse = parse(content.substring(i), parser)
            if (tryParse != null) {
                i += tryParse.trueLength-1
            }
            i++
        }
        return i
    }
}

class EmptyXmlNode(val key: String, val spaceBefore: Boolean = false, val parameters: String? = null) : MarkdownNode {
    override val length: Int
        get() = 0
    override val trueLength: Int
        get() = 3 + key.length + (if (spaceBefore) 1 else 0) + (parameters?.length ?: -1) + 1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmptyXmlNode) return false
        return key == other.key && spaceBefore == other.spaceBefore && parameters == other.parameters
    }

    override fun toString(): String {
        if (parameters != null)
            return "<$key $parameters/>"
        if (spaceBefore)
            return "<$key />"
        return "<$key/>"
    }

    override fun toTreeString(indent: Int): String {
        return "Xml: " + " ".repeat(indent) + "$key ${parameters ?: ""}"
    }

    override fun truncate(length: Int): MarkdownNode {
        return this
    }

    override fun hashCode(): Int = 31 * (31 * key.hashCode() + spaceBefore.hashCode()) + parameters.hashCode()
}

class XmlNode(val key: String, val nodes: MutableList<MarkdownNode> = arrayListOf(), val spaceBefore: Boolean = false, val parameters: String? = null) : MarkdownNode {
    constructor(key: String, content: String, parser: MarkdownParser, spaceBefore: Boolean = false, parameters: String?) : this(key, parser.parse(content).nodes, spaceBefore, parameters)

    override val length: Int
        get() = nodes.sumOf(MarkdownNode::length)
    override val trueLength: Int
        get() = 5 + key.length*2 + (if (spaceBefore) 1 else 0) + (parameters?.length ?: -1) + 1 + nodes.sumOf(MarkdownNode::trueLength)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is XmlNode) return false
        return key == other.key && spaceBefore == other.spaceBefore && parameters == other.parameters && nodes == other.nodes
    }

    override fun toString(): String {
        var content = ""
        for (node in nodes) {
            content += node
        }
        if (parameters != null)
            return "<$key $parameters>$content</$key>"
        if (spaceBefore)
            return "<$key >$content</$key>"
        return "<$key>$content</$key>"
    }

    override fun toTreeString(indent: Int): String {
        var out = "Xml: " + " ".repeat(indent) + "$key ${parameters ?: ""}\n"
        for (node in nodes) {
            out += node.toTreeString(indent+1)+"\n"
        }
        return out.substring(0, out.length-1)
    }

    override fun truncate(length: Int): MarkdownNode {
        val new = XmlNode(key, arrayListOf(), spaceBefore, parameters)

        var accumulator = length

        for (node in nodes) {
            if (accumulator == 0) break
            if (node.length == accumulator) {
                new.nodes.add(node)
                break
            }
            if (node.length > accumulator) {
                new.nodes.add(node.truncate(accumulator))
                break
            }
            new.nodes.add(node)
            accumulator -= node.length
        }

        return new
    }

    override fun hashCode(): Int = 31 * (31 * key.hashCode() + spaceBefore.hashCode()) + parameters.hashCode()
}
