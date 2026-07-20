package org.tiqian.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownCompilerTest {
    private val compiler = MarkdownCompiler()

    @Test
    fun lowersCommonTextBlocksAndInlineSemantics() {
        val markdown = """
            # 标题

            正文有 **粗体**、*强调* 和 [链接](https://example.com)。

            > 引用

            3. 第一项
            4. 第二项
        """.trimIndent()

        val document = compiler.compile(markdown)

        val heading = assertIs<MarkdownHeading>(document.blocks[0])
        assertEquals(1, heading.level)
        assertEquals("标题", heading.text.value)

        val paragraph = assertIs<MarkdownParagraph>(document.blocks[1])
        assertEquals("正文有 粗体、强调 和 链接。", paragraph.text.value)
        assertTrue(paragraph.text.spans.any { it.mark == MarkdownTextMark.Strong })
        assertTrue(paragraph.text.spans.any { it.mark == MarkdownTextMark.Emphasis })
        assertTrue(
            paragraph.text.spans.any {
                it.mark == MarkdownTextMark.Link("https://example.com")
            },
        )

        val quote = assertIs<MarkdownBlockQuote>(document.blocks[2])
        assertEquals("引用", assertIs<MarkdownParagraph>(quote.blocks.single()).text.value)

        val list = assertIs<MarkdownList>(document.blocks[3])
        assertTrue(list.ordered)
        assertEquals(3, list.startNumber)
        assertEquals(2, list.items.size)
        assertEquals("第一项", assertIs<MarkdownParagraph>(list.items[0].blocks.single()).text.value)
        assertTrue(document.issues.isEmpty())
    }

    @Test
    fun preservesSourceAndUsesBlockScopedKeys() {
        val markdown = "第一段\n\n第二段"

        val document = compiler.compile(markdown)
        val first = assertIs<MarkdownParagraph>(document.blocks[0])
        val second = assertIs<MarkdownParagraph>(document.blocks[1])

        assertEquals("第一段", first.metadata.sourceMarkdown)
        assertEquals("第二段", second.metadata.sourceMarkdown)
        assertTrue(first.metadata.key != second.metadata.key)
        assertEquals(0, first.metadata.sourceSpan.startOffset)
        assertEquals(markdown.length, second.metadata.sourceSpan.endOffset)
    }

    @Test
    fun softBreakCollapsesAndHardBreakRemains() {
        val markdown = "软\n换行  \n硬换行"

        val paragraph = assertIs<MarkdownParagraph>(compiler.compile(markdown).blocks.single())

        assertEquals("软 换行\n硬换行", paragraph.text.value)
    }

    @Test
    fun unsupportedInlineIsReportedWithoutDroppingReadableText() {
        val markdown = "公式 ${'$'}x + y${'$'} 仍然可读"

        val document = compiler.compile(markdown)
        val paragraph = assertIs<MarkdownParagraph>(document.blocks.single())

        assertEquals("公式 x + y 仍然可读", paragraph.text.value)
        assertEquals(1, paragraph.text.issues.size)
        assertEquals(MarkdownCapabilityIssueKind.UnsupportedInline, paragraph.text.issues.single().kind)
        assertEquals("InlineMath", paragraph.text.issues.single().nodeType)
        assertEquals(paragraph.text.issues, document.issues)
    }

    @Test
    fun unsupportedBlockBecomesAnExplicitFallbackBlock() {
        val markdown = "| 甲 | 乙 |\n| --- | --- |\n| 一 | 二 |"

        val document = compiler.compile(markdown)
        val block = assertIs<MarkdownUnsupportedBlock>(document.blocks.single())

        assertEquals("Table", block.nodeType)
        assertEquals(markdown, block.fallbackText)
        assertEquals(MarkdownCapabilityIssueKind.UnsupportedBlock, document.issues.single().kind)
    }
}
