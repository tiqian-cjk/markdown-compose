package org.tiqian.markdown

/** A renderer-owned document model. Parser nodes never cross this boundary. */
data class MarkdownRenderDocument(
    val blocks: List<MarkdownBlock>,
    val issues: List<MarkdownCapabilityIssue> = emptyList(),
)

data class MarkdownNodeKey(
    val parserStableKey: Int,
    val path: List<Int>,
)

data class MarkdownSourceSpan(
    val startOffset: Int,
    val endOffset: Int,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
)

data class MarkdownNodeMetadata(
    val key: MarkdownNodeKey,
    val sourceSpan: MarkdownSourceSpan,
    /** Exact Markdown covered by this node when the compiler was given the source text. */
    val sourceMarkdown: String? = null,
)

sealed interface MarkdownBlock {
    val metadata: MarkdownNodeMetadata
}

data class MarkdownParagraph(
    val text: MarkdownText,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownHeading(
    val level: Int,
    val id: String?,
    val text: MarkdownText,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownBlockQuote(
    val blocks: List<MarkdownBlock>,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownList(
    val ordered: Boolean,
    val startNumber: Int,
    val tight: Boolean,
    val items: List<MarkdownListItem>,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownListItem(
    val blocks: List<MarkdownBlock>,
    val task: MarkdownTaskState? = null,
    val metadata: MarkdownNodeMetadata,
)

enum class MarkdownTaskState {
    Checked,
    Unchecked,
}

data class MarkdownCodeBlock(
    val code: String,
    val language: String?,
    val info: String?,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownThematicBreak(
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownImageBlock(
    val destination: String,
    val description: String,
    val title: String?,
    val widthPixels: Int?,
    val heightPixels: Int?,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

/**
 * A loss-shaped boundary for syntax that this version does not render natively yet.
 * Hosts can replace it through a block slot; the default renderer keeps readable content visible.
 */
data class MarkdownUnsupportedBlock(
    val nodeType: String,
    val fallbackText: String,
    override val metadata: MarkdownNodeMetadata,
) : MarkdownBlock

data class MarkdownText(
    val value: String,
    val spans: List<MarkdownTextSpan> = emptyList(),
    val issues: List<MarkdownCapabilityIssue> = emptyList(),
)

data class MarkdownTextRange(
    val start: Int,
    val endExclusive: Int,
) {
    init {
        require(start >= 0) { "start must not be negative" }
        require(endExclusive >= start) { "endExclusive must not precede start" }
    }
}

data class MarkdownTextSpan(
    val range: MarkdownTextRange,
    val mark: MarkdownTextMark,
)

sealed interface MarkdownTextMark {
    data object Strong : MarkdownTextMark
    data object Emphasis : MarkdownTextMark
    data object Strikethrough : MarkdownTextMark
    data object InlineCode : MarkdownTextMark
    data object Highlight : MarkdownTextMark
    data object Superscript : MarkdownTextMark
    data object Subscript : MarkdownTextMark
    data object Inserted : MarkdownTextMark

    data class Link(
        val destination: String,
        val title: String? = null,
    ) : MarkdownTextMark

    data class Abbreviation(
        val fullText: String,
    ) : MarkdownTextMark

    data class Footnote(
        val label: String,
        val index: Int,
    ) : MarkdownTextMark

    data class Ruby(
        val annotation: String,
    ) : MarkdownTextMark
}

enum class MarkdownCapabilityIssueKind {
    UnsupportedBlock,
    UnsupportedInline,
}

data class MarkdownCapabilityIssue(
    val kind: MarkdownCapabilityIssueKind,
    val nodeType: String,
    val sourceSpan: MarkdownSourceSpan,
    val textRange: MarkdownTextRange? = null,
)
