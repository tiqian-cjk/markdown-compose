package org.tiqian.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
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
}
