package org.tiqian.markdown

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.Abbreviation
import com.hrm.markdown.parser.ast.AbbreviationDefinition
import com.hrm.markdown.parser.ast.Autolink
import com.hrm.markdown.parser.ast.BibliographyDefinition
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.CitationReference
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emoji
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.EscapedChar
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.FrontMatter
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.HtmlEntity
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.IndentedCodeBlock
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineHtml
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.InsertedText
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.LeafNode
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.LinkReferenceDefinition
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Spoiler
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.StyledText
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.ThematicBreak
import com.hrm.markdown.parser.ast.WikiLink

/** Compiles parser output into the renderer-owned, Compose-free model. */
class MarkdownCompiler(
    private val parser: MarkdownParser = MarkdownParser(),
) {
    fun compile(markdown: String): MarkdownRenderDocument =
        compile(parser.parse(markdown), markdown)

    fun compile(document: Document, sourceMarkdown: String? = null): MarkdownRenderDocument {
        val issues = mutableListOf<MarkdownCapabilityIssue>()
        val sourceLocator = sourceMarkdown?.let(::MarkdownSourceLocator)
        val blocks = document.children.mapIndexedNotNull { index, node ->
            compileBlock(node, listOf(index), sourceMarkdown, sourceLocator, issues)
        }
        return MarkdownRenderDocument(blocks = blocks, issues = issues)
    }

    private fun compileBlock(
        node: Node,
        path: List<Int>,
        source: String?,
        sourceLocator: MarkdownSourceLocator?,
        issues: MutableList<MarkdownCapabilityIssue>,
    ): MarkdownBlock? {
        val metadata = node.metadata(path, source, sourceLocator)
        return when (node) {
            is Paragraph -> MarkdownParagraph(
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is Heading -> MarkdownHeading(
                level = node.level.coerceIn(1, 6),
                id = node.id,
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is SetextHeading -> MarkdownHeading(
                level = node.level.coerceIn(1, 6),
                id = node.id,
                text = compileText(node, metadata.sourceSpan, sourceLocator, issues),
                metadata = metadata,
            )

            is BlockQuote -> MarkdownBlockQuote(
                blocks = compileBlockChildren(node, path, source, sourceLocator, issues),
                metadata = metadata,
            )

            is ListBlock -> MarkdownList(
                ordered = node.ordered,
                startNumber = node.startNumber,
                tight = node.tight,
                items = node.children.mapIndexedNotNull { index, child ->
                    val item = child as? ListItem ?: return@mapIndexedNotNull null
                    MarkdownListItem(
                        blocks = compileBlockChildren(item, path + index, source, sourceLocator, issues),
                        task = if (!item.taskListItem) {
                            null
                        } else if (item.checked) {
                            MarkdownTaskState.Checked
                        } else {
                            MarkdownTaskState.Unchecked
                        },
                        metadata = item.metadata(path + index, source, sourceLocator),
                    )
                },
                metadata = metadata,
            )

            is FencedCodeBlock -> MarkdownCodeBlock(
                code = node.literal,
                language = node.language.ifBlank { null },
                info = node.info.ifBlank { null },
                metadata = metadata,
            )

            is IndentedCodeBlock -> MarkdownCodeBlock(
                code = node.literal,
                language = null,
                info = null,
                metadata = metadata,
            )

            is ThematicBreak -> MarkdownThematicBreak(metadata)

            is Figure -> MarkdownImageBlock(
                destination = node.imageUrl,
                description = node.caption,
                title = node.caption.ifBlank { null },
                widthPixels = node.imageWidth,
                heightPixels = node.imageHeight,
                metadata = metadata,
            )

            // Definitions and front matter affect the document but do not produce visible blocks.
            is LinkReferenceDefinition,
            is AbbreviationDefinition,
            is BibliographyDefinition,
            is BlankLine,
            is FrontMatter,
            -> null

            else -> {
                issues += MarkdownCapabilityIssue(
                    kind = MarkdownCapabilityIssueKind.UnsupportedBlock,
                    nodeType = node.typeName(),
                    sourceSpan = metadata.sourceSpan,
                )
                MarkdownUnsupportedBlock(
                    nodeType = node.typeName(),
                    fallbackText = metadata.sourceMarkdown ?: node.readableText(),
                    metadata = metadata,
                )
            }
        }
    }

    private fun compileBlockChildren(
        node: ContainerNode,
        path: List<Int>,
        source: String?,
        sourceLocator: MarkdownSourceLocator?,
        issues: MutableList<MarkdownCapabilityIssue>,
    ): List<MarkdownBlock> = node.children.mapIndexedNotNull { index, child ->
        compileBlock(child, path + index, source, sourceLocator, issues)
    }

    private fun compileText(
        node: ContainerNode,
        sourceSpan: MarkdownSourceSpan,
        sourceLocator: MarkdownSourceLocator?,
        documentIssues: MutableList<MarkdownCapabilityIssue>,
    ): MarkdownText {
        val builder = MarkdownTextBuilder(documentIssues, sourceSpan, sourceLocator)
        node.children.forEach(builder::append)
        return builder.build()
    }
}

private class MarkdownTextBuilder(
    private val documentIssues: MutableList<MarkdownCapabilityIssue>,
    private val blockSourceSpan: MarkdownSourceSpan,
    private val sourceLocator: MarkdownSourceLocator?,
) {
    private val value = StringBuilder()
    private val spans = mutableListOf<MarkdownTextSpan>()
    private val issues = mutableListOf<MarkdownCapabilityIssue>()

    fun append(node: Node) {
        when (node) {
            is Text -> value.append(node.literal)
            is SoftLineBreak -> value.append(' ')
            is HardLineBreak -> value.append('\n')
            is HtmlEntity -> value.append(node.resolved.ifEmpty { node.literal })
            is EscapedChar -> value.append(node.literal)
            is Emoji -> value.append(node.unicode ?: node.literal)
            is InlineCode -> marked(node, MarkdownTextMark.InlineCode) { value.append(node.literal) }
            is Emphasis -> markedChildren(node, MarkdownTextMark.Emphasis)
            is StrongEmphasis -> markedChildren(node, MarkdownTextMark.Strong)
            is Strikethrough -> markedChildren(node, MarkdownTextMark.Strikethrough)
            is Highlight -> markedChildren(node, MarkdownTextMark.Highlight)
            is Superscript -> markedChildren(node, MarkdownTextMark.Superscript)
            is Subscript -> markedChildren(node, MarkdownTextMark.Subscript)
            is InsertedText -> markedChildren(node, MarkdownTextMark.Inserted)
            is Link -> markedChildren(node, MarkdownTextMark.Link(node.destination, node.title))
            is Autolink -> marked(node, MarkdownTextMark.Link(node.destination)) { value.append(node.literal) }
            is WikiLink -> marked(node, MarkdownTextMark.Link(node.target)) { value.append(node.literal) }
            is Abbreviation -> marked(node, MarkdownTextMark.Abbreviation(node.fullText)) {
                value.append(node.literal)
            }
            is FootnoteReference -> marked(node, MarkdownTextMark.Footnote(node.label, node.index)) {
                value.append(node.label)
            }
            is RubyText -> marked(node, MarkdownTextMark.Ruby(node.annotation)) { value.append(node.base) }
            is KeyboardInput -> marked(node, MarkdownTextMark.InlineCode) { value.append(node.literal) }
            is Image -> unsupported(node) {
                node.children.forEach(::append)
                if (node.children.isEmpty()) value.append(node.title ?: node.destination)
            }
            is InlineMath -> unsupported(node) { value.append(node.literal) }
            is InlineHtml -> unsupported(node) { value.append(node.literal) }
            is StyledText -> unsupported(node) { node.children.forEach(::append) }
            is Spoiler -> unsupported(node) { node.children.forEach(::append) }
            is CitationReference -> unsupported(node) { value.append("[@${node.key}]") }
            is DirectiveInline -> unsupported(node) { value.append(node.literal) }
            is ContainerNode -> unsupported(node) { node.children.forEach(::append) }
            is LeafNode -> unsupported(node) { value.append(node.literal) }
        }
    }

    fun build(): MarkdownText = MarkdownText(
        value = value.toString(),
        spans = spans.toList(),
        issues = issues.toList(),
    )

    private fun markedChildren(node: ContainerNode, mark: MarkdownTextMark) {
        marked(node, mark) { node.children.forEach(::append) }
    }

    private inline fun marked(node: Node, mark: MarkdownTextMark, content: () -> Unit) {
        val start = value.length
        content()
        if (value.length > start) {
            spans += MarkdownTextSpan(MarkdownTextRange(start, value.length), mark)
        }
    }

    private inline fun unsupported(node: Node, content: () -> Unit) {
        val start = value.length
        content()
        val issue = MarkdownCapabilityIssue(
            kind = MarkdownCapabilityIssueKind.UnsupportedInline,
            nodeType = node.typeName(),
            sourceSpan = node.sourceSpan(sourceLocator, blockSourceSpan),
            textRange = MarkdownTextRange(start, value.length),
        )
        issues += issue
        documentIssues += issue
    }
}

private fun Node.metadata(
    path: List<Int>,
    source: String?,
    sourceLocator: MarkdownSourceLocator?,
): MarkdownNodeMetadata {
    val span = sourceSpan(sourceLocator)
    val sourceSlice = source?.takeIf {
        span.startOffset in 0..it.length && span.endOffset in span.startOffset..it.length
    }?.substring(span.startOffset, span.endOffset)
    return MarkdownNodeMetadata(
        key = MarkdownNodeKey(parserStableKey = stableKey, path = path),
        sourceSpan = span,
        sourceMarkdown = sourceSlice,
    )
}

private fun Node.sourceSpan(
    locator: MarkdownSourceLocator?,
    fallback: MarkdownSourceSpan? = null,
): MarkdownSourceSpan {
    if (sourceRange.length > 0) {
        return MarkdownSourceSpan(
            startOffset = sourceRange.start.offset,
            endOffset = sourceRange.end.offset,
            startLine = sourceRange.start.line,
            startColumn = sourceRange.start.column,
            endLine = sourceRange.end.line,
            endColumn = sourceRange.end.column,
        )
    }
    if (locator != null && lineRange.endLine > lineRange.startLine) {
        return locator.span(lineRange.startLine, lineRange.endLine)
    }
    return fallback ?: MarkdownSourceSpan(0, 0, 0, 0, 0, 0)
}

private fun Node.typeName(): String = this::class.simpleName ?: "UnknownNode"

private fun Node.readableText(): String = when (this) {
    is HtmlEntity -> resolved.ifEmpty { literal }
    is Emoji -> unicode ?: literal
    is LeafNode -> literal
    is ContainerNode -> children.joinToString(separator = "") { it.readableText() }
}

private class MarkdownSourceLocator(
    private val source: String,
) {
    private val lineStarts: List<Int> = buildList {
        add(0)
        source.forEachIndexed { index, char ->
            if (char == '\n') add(index + 1)
        }
    }

    fun span(startLine: Int, endLineExclusive: Int): MarkdownSourceSpan {
        val safeStartLine = startLine.coerceIn(0, lineStarts.lastIndex)
        val safeLastLine = (endLineExclusive - 1).coerceIn(safeStartLine, lineStarts.lastIndex)
        val startOffset = lineStarts[safeStartLine]
        val endOffset = lineEndOffset(safeLastLine)
        return MarkdownSourceSpan(
            startOffset = startOffset,
            endOffset = endOffset,
            startLine = safeStartLine,
            startColumn = 0,
            endLine = safeLastLine,
            endColumn = endOffset - lineStarts[safeLastLine],
        )
    }

    private fun lineEndOffset(line: Int): Int {
        if (line == lineStarts.lastIndex) return source.length
        var end = lineStarts[line + 1]
        if (end > 0 && source[end - 1] == '\n') end--
        if (end > 0 && source[end - 1] == '\r') end--
        return end
    }
}
