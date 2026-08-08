package org.tiqian.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownAnnotatedStringTest {
    @Test
    fun lowersOverlappingStylesAndLinks() {
        val text = MarkdownText(
            value = "粗体链接",
            spans = listOf(
                MarkdownTextSpan(MarkdownTextRange(0, 4), MarkdownTextMark.Strong),
                MarkdownTextSpan(
                    MarkdownTextRange(2, 4),
                    MarkdownTextMark.Link("https://example.com"),
                ),
            ),
        )

        val annotated = text.toAnnotatedString(
            MarkdownStyle(link = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline)),
        )

        assertEquals("粗体链接", annotated.text)
        assertTrue(annotated.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        val link = annotated.getLinkAnnotations(0, annotated.length).single().item
        assertEquals("https://example.com", (link as LinkAnnotation.Url).url)
    }

    @Test
    fun rubyKeepsBaseTextAndTiqianAnnotation() {
        val text = MarkdownText(
            value = "漢字",
            spans = listOf(
                MarkdownTextSpan(MarkdownTextRange(0, 2), MarkdownTextMark.Ruby("かんじ")),
            ),
        )

        val annotated = text.toAnnotatedString(MarkdownStyle())

        assertEquals("漢字", annotated.text)
        assertEquals("かんじ", annotated.getStringAnnotations(0, annotated.length).single().item)
    }

    @Test
    fun abbreviationKeepsExpandedTextAnnotation() {
        val text = MarkdownText(
            value = "CLREQ",
            spans = listOf(
                MarkdownTextSpan(
                    MarkdownTextRange(0, 5),
                    MarkdownTextMark.Abbreviation("Requirements for Chinese Text Layout"),
                ),
            ),
        )

        val annotated = text.toAnnotatedString(MarkdownStyle())

        assertEquals(
            "Requirements for Chinese Text Layout",
            annotated.getStringAnnotations("abbreviation", 0, annotated.length).single().item,
        )
    }

    @Test
    fun keyboardAndFootnoteUseDedicatedStyles() {
        val keyboardStyle = SpanStyle(color = Color.Red, fontWeight = FontWeight.Medium)
        val footnoteStyle = SpanStyle(color = Color.Green)
        val text = MarkdownText(
            value = "Ctrl[1]",
            spans = listOf(
                MarkdownTextSpan(MarkdownTextRange(0, 4), MarkdownTextMark.KeyboardInput),
                MarkdownTextSpan(MarkdownTextRange(4, 7), MarkdownTextMark.Footnote("1", 1)),
            ),
        )

        val annotated = text.toAnnotatedString(
            MarkdownStyle(
                keyboardInput = keyboardStyle,
                footnote = footnoteStyle,
            ),
        )

        assertTrue(annotated.spanStyles.any { it.start == 0 && it.end == 4 && it.item.color == Color.Red })
        assertTrue(
            annotated.spanStyles.any {
                it.start == 4 && it.end == 7 &&
                    it.item.color == Color.Green &&
                    it.item.baselineShift == BaselineShift.Superscript
            },
        )
        assertTrue(annotated.getLinkAnnotations(4, 7).single().item is LinkAnnotation.Clickable)
    }
}
