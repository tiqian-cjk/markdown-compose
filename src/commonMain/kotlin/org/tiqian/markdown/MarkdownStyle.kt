package org.tiqian.markdown

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.tiqian.core.Ic
import org.tiqian.core.ic

@Immutable
data class MarkdownStyle(
    val body: TextStyle = TextStyle(
        color = Color(0xFF202124),
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    val heading1: TextStyle = body.merge(TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)),
    val heading2: TextStyle = body.merge(TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)),
    val heading3: TextStyle = body.merge(TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)),
    val heading4: TextStyle = body.merge(TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)),
    val heading5: TextStyle = body.merge(TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)),
    val heading6: TextStyle = body.merge(TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)),
    val codeBlock: TextStyle = body.merge(
        TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 22.sp,
        ),
    ),
    val link: SpanStyle = SpanStyle(
        color = Color(0xFF0969DA),
        textDecoration = TextDecoration.Underline,
    ),
    val inlineCode: SpanStyle = SpanStyle(
        background = Color(0xFFF1F3F5),
        fontFamily = FontFamily.Monospace,
        fontSize = 0.92.em,
    ),
    val highlight: SpanStyle = SpanStyle(background = Color(0xFFFFE58F)),
    val abbreviation: SpanStyle = SpanStyle(textDecoration = TextDecoration.Underline),
    val footnote: SpanStyle = SpanStyle(fontSize = 0.8.em),
    val keyboardInput: SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 0.8.em,
        fontWeight = FontWeight.Medium,
    ),
    val blockSpacing: Dp = 16.dp,
    val compactBlockSpacing: Dp = 6.dp,
    val quoteBarColor: Color = Color(0xFFD0D7DE),
    val quoteBarWidth: Dp = 3.dp,
    val quoteContentPadding: Dp = 12.dp,
    /** Minimum body indent when the list measure is not narrow. */
    val listContentIndent: Ic = 2.ic,
    /** Minimum body indent below [listNarrowBreakpoint]. */
    val listNarrowContentIndent: Ic = 1.ic,
    /** Measures narrower than this many CJK cells use [listNarrowContentIndent]. */
    val listNarrowBreakpoint: Ic = 20.ic,
    val listItemSpacing: Dp = 8.dp,
    val codeBackground: Color = Color(0xFFF6F8FA),
    val codePadding: Dp = 12.dp,
    val mathBackground: Color = codeBackground,
    val math: MarkdownMathStyle = MarkdownMathStyle(),
    val tableBorderColor: Color = Color(0xFFD8DEE4),
    val tableHeaderBackground: Color = Color(0xFFF6F8FA),
    val tableCellPadding: Dp = 8.dp,
    val tableColumnWidth: Dp = 160.dp,
    val footnoteLabelWidth: Dp = 32.dp,
    val thematicBreakColor: Color = Color(0xFFD8DEE4),
)

internal fun MarkdownStyle.heading(level: Int): TextStyle = when (level) {
    1 -> heading1
    2 -> heading2
    3 -> heading3
    4 -> heading4
    5 -> heading5
    else -> heading6
}
