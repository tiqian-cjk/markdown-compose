package org.tiqian.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.tiqian.compose.CjkText
import org.tiqian.compose.cjkTextCompatibility
import org.tiqian.compose.ruby

enum class MarkdownTextFallbackPolicy {
    /** Use Compose text whenever a text block contains semantics Tiqian cannot preserve. */
    Automatic,

    /** Always use Tiqian. Intended for capability dogfooding. */
    Disabled,
}

/** Slots for non-prose blocks. The default set never performs network loading. */
class MarkdownBlockSlots(
    val codeBlock: (@Composable (MarkdownCodeBlock, MarkdownStyle) -> Unit)? = null,
    val imageBlock: (@Composable (MarkdownImageBlock, MarkdownStyle) -> Unit)? = null,
    val thematicBreak: (@Composable (MarkdownThematicBreak, MarkdownStyle) -> Unit)? = null,
    val unsupportedBlock: (@Composable (MarkdownUnsupportedBlock, MarkdownStyle) -> Unit)? = null,
)

val DefaultMarkdownBlockSlots: MarkdownBlockSlots = MarkdownBlockSlots()

/** Parses and renders Markdown content. This composable intentionally does not own scrolling. */
@Composable
fun TiqianMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    style: MarkdownStyle = MarkdownStyle(),
    slots: MarkdownBlockSlots = DefaultMarkdownBlockSlots,
    fallbackPolicy: MarkdownTextFallbackPolicy = MarkdownTextFallbackPolicy.Automatic,
    onLinkClick: ((String) -> Unit)? = null,
    compiler: MarkdownCompiler? = null,
) {
    val defaultCompiler = remember { MarkdownCompiler() }
    val resolvedCompiler = compiler ?: defaultCompiler
    val document = remember(markdown, resolvedCompiler) { resolvedCompiler.compile(markdown) }
    TiqianMarkdown(
        document = document,
        modifier = modifier,
        style = style,
        slots = slots,
        fallbackPolicy = fallbackPolicy,
        onLinkClick = onLinkClick,
    )
}

/** Renders an already compiled document without parsing it again. */
@Composable
fun TiqianMarkdown(
    document: MarkdownRenderDocument,
    modifier: Modifier = Modifier,
    style: MarkdownStyle = MarkdownStyle(),
    slots: MarkdownBlockSlots = DefaultMarkdownBlockSlots,
    fallbackPolicy: MarkdownTextFallbackPolicy = MarkdownTextFallbackPolicy.Automatic,
    onLinkClick: ((String) -> Unit)? = null,
) {
    MarkdownBlocks(
        blocks = document.blocks,
        modifier = modifier,
        style = style,
        slots = slots,
        fallbackPolicy = fallbackPolicy,
        onLinkClick = onLinkClick,
        compact = false,
    )
}

@Composable
private fun MarkdownBlocks(
    blocks: List<MarkdownBlock>,
    modifier: Modifier,
    style: MarkdownStyle,
    slots: MarkdownBlockSlots,
    fallbackPolicy: MarkdownTextFallbackPolicy,
    onLinkClick: ((String) -> Unit)?,
    compact: Boolean,
) {
    val spacing = if (compact) style.compactBlockSpacing else style.blockSpacing
    Column(modifier) {
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(spacing))
            key(block.metadata.key) {
                MarkdownBlock(
                    block = block,
                    style = style,
                    slots = slots,
                    fallbackPolicy = fallbackPolicy,
                    onLinkClick = onLinkClick,
                )
            }
        }
    }
}

@Composable
private fun MarkdownBlock(
    block: MarkdownBlock,
    style: MarkdownStyle,
    slots: MarkdownBlockSlots,
    fallbackPolicy: MarkdownTextFallbackPolicy,
    onLinkClick: ((String) -> Unit)?,
) {
    when (block) {
        is MarkdownParagraph -> MarkdownTextBlock(block.text, style.body, style, fallbackPolicy, onLinkClick)
        is MarkdownHeading -> MarkdownTextBlock(block.text, style.heading(block.level), style, fallbackPolicy, onLinkClick)
        is MarkdownBlockQuote -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Spacer(
                Modifier
                    .width(style.quoteBarWidth)
                    .fillMaxHeight()
                    .background(style.quoteBarColor),
            )
            MarkdownBlocks(
                blocks = block.blocks,
                modifier = Modifier.weight(1f).padding(start = style.quoteContentPadding),
                style = style,
                slots = slots,
                fallbackPolicy = fallbackPolicy,
                onLinkClick = onLinkClick,
                compact = true,
            )
        }

        is MarkdownList -> MarkdownListBlock(block, style, slots, fallbackPolicy, onLinkClick)
        is MarkdownCodeBlock -> slots.codeBlock?.invoke(block, style) ?: DefaultMarkdownCodeBlock(block, style)
        is MarkdownImageBlock -> slots.imageBlock?.invoke(block, style)
            ?: DefaultMarkdownImageBlock(block, style, onLinkClick)
        is MarkdownThematicBreak -> slots.thematicBreak?.invoke(block, style) ?: DefaultMarkdownThematicBreak(style)
        is MarkdownUnsupportedBlock -> slots.unsupportedBlock?.invoke(block, style)
            ?: DefaultMarkdownUnsupportedBlock(block, style)
    }
}

@Composable
private fun MarkdownListBlock(
    block: MarkdownList,
    style: MarkdownStyle,
    slots: MarkdownBlockSlots,
    fallbackPolicy: MarkdownTextFallbackPolicy,
    onLinkClick: ((String) -> Unit)?,
) {
    Column {
        block.items.forEachIndexed { index, item ->
            if (index > 0) Spacer(Modifier.height(if (block.tight) style.compactBlockSpacing else style.listItemSpacing))
            Row(Modifier.fillMaxWidth()) {
                val marker = when (item.task) {
                    MarkdownTaskState.Checked -> "[x]"
                    MarkdownTaskState.Unchecked -> "[ ]"
                    null -> if (block.ordered) "${block.startNumber + index}." else "•"
                }
                BasicText(
                    text = marker,
                    modifier = Modifier.widthIn(min = style.listMarkerWidth),
                    style = style.body,
                )
                MarkdownBlocks(
                    blocks = item.blocks,
                    modifier = Modifier.weight(1f),
                    style = style,
                    slots = slots,
                    fallbackPolicy = fallbackPolicy,
                    onLinkClick = onLinkClick,
                    compact = block.tight,
                )
            }
        }
    }
}

@Composable
private fun MarkdownTextBlock(
    text: MarkdownText,
    textStyle: TextStyle,
    markdownStyle: MarkdownStyle,
    fallbackPolicy: MarkdownTextFallbackPolicy,
    onLinkClick: ((String) -> Unit)?,
) {
    val annotated = remember(text, markdownStyle, onLinkClick) {
        text.toAnnotatedString(markdownStyle, onLinkClick)
    }
    val compatibility = annotated.cjkTextCompatibility(textStyle)
    val shouldFallback = fallbackPolicy == MarkdownTextFallbackPolicy.Automatic &&
        (text.issues.isNotEmpty() || !compatibility.canPreserveAllKnownSemantics)
    if (shouldFallback) {
        BasicText(text = annotated, style = textStyle)
    } else {
        CjkText(text = annotated, style = textStyle)
    }
}

internal fun MarkdownText.toAnnotatedString(
    style: MarkdownStyle,
    onLinkClick: ((String) -> Unit)? = null,
): AnnotatedString {
    val rubySpans = spans
        .filter { it.mark is MarkdownTextMark.Ruby }
        .sortedBy { it.range.start }
    val withRuby = AnnotatedString.Builder().apply {
        var cursor = 0
        rubySpans.forEach { span ->
            if (span.range.start < cursor || span.range.endExclusive > value.length) return@forEach
            append(value.substring(cursor, span.range.start))
            ruby(
                base = value.substring(span.range.start, span.range.endExclusive),
                ruby = (span.mark as MarkdownTextMark.Ruby).annotation,
            )
            cursor = span.range.endExclusive
        }
        append(value.substring(cursor))
    }.toAnnotatedString()

    return AnnotatedString.Builder(withRuby).apply {
        spans.forEach { span ->
            val start = span.range.start.coerceIn(0, length)
            val end = span.range.endExclusive.coerceIn(start, length)
            when (val mark = span.mark) {
                MarkdownTextMark.Strong -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                MarkdownTextMark.Emphasis -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                MarkdownTextMark.Strikethrough -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.LineThrough),
                    start,
                    end,
                )

                MarkdownTextMark.InlineCode -> addStyle(style.inlineCode, start, end)
                MarkdownTextMark.Highlight -> addStyle(style.highlight, start, end)
                MarkdownTextMark.Superscript -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.8.em),
                    start,
                    end,
                )

                MarkdownTextMark.Subscript -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.8.em),
                    start,
                    end,
                )

                MarkdownTextMark.Inserted -> addStyle(
                    SpanStyle(textDecoration = TextDecoration.Underline),
                    start,
                    end,
                )

                is MarkdownTextMark.Link -> addLink(
                    LinkAnnotation.Url(
                        url = mark.destination,
                        styles = TextLinkStyles(style = style.link),
                        linkInteractionListener = onLinkClick?.let { callback ->
                            LinkInteractionListener { link ->
                                if (link is LinkAnnotation.Url) callback(link.url)
                            }
                        },
                    ),
                    start,
                    end,
                )

                is MarkdownTextMark.Abbreviation -> addStyle(style.abbreviation, start, end)
                is MarkdownTextMark.Footnote -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.8.em),
                    start,
                    end,
                )

                is MarkdownTextMark.Ruby -> Unit
            }
        }
    }.toAnnotatedString()
}

@Composable
fun DefaultMarkdownCodeBlock(block: MarkdownCodeBlock, style: MarkdownStyle) {
    BasicText(
        text = block.code,
        modifier = Modifier.fillMaxWidth().background(style.codeBackground).padding(style.codePadding),
        style = style.codeBlock,
    )
}

@Composable
fun DefaultMarkdownImageBlock(
    block: MarkdownImageBlock,
    style: MarkdownStyle,
    onLinkClick: ((String) -> Unit)? = null,
) {
    val label = block.description.ifBlank { block.destination }
    MarkdownTextBlock(
        text = MarkdownText(
            value = label,
            spans = listOf(
                MarkdownTextSpan(
                    MarkdownTextRange(0, label.length),
                    MarkdownTextMark.Link(block.destination, block.title),
                ),
            ),
        ),
        textStyle = style.body,
        markdownStyle = style,
        fallbackPolicy = MarkdownTextFallbackPolicy.Automatic,
        onLinkClick = onLinkClick,
    )
}

@Composable
fun DefaultMarkdownThematicBreak(style: MarkdownStyle) {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(style.thematicBreakColor))
}

@Composable
fun DefaultMarkdownUnsupportedBlock(block: MarkdownUnsupportedBlock, style: MarkdownStyle) {
    val fallback = block.fallbackText.ifBlank { block.metadata.sourceMarkdown.orEmpty() }
    BasicText(text = fallback, style = style.body)
}
