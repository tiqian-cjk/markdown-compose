package org.tiqian.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalComposeUiApi::class)
class MarkdownListAlignmentTest {
    @Test
    fun orderedMarkerSharesFirstLineBaselineWithTiqianBody() {
        render(document(start = 1, itemTexts = listOf("1.")), width = 600).let { pixels ->
            val markerInk = pixels.inkRows(24 until 48)
            val bodyInk = pixels.inkRows(48 until 100)

            assertTrue(markerInk.isNotEmpty(), "list marker did not render")
            assertTrue(bodyInk.isNotEmpty(), "list body did not render")
            assertEquals(markerInk, bodyInk)
        }
    }

    @Test
    fun contentIndentDropsFromTwoEmToOneEmOnlyOnNarrowMeasures() {
        val document = document(start = 1, itemTexts = listOf("1."))
        val wide = render(document, width = 600)
        val narrow = render(document, width = 300)

        val wideMarkerStart = wide.inkMinX(0 until 48)
        val narrowMarkerStart = narrow.inkMinX(0 until 24)
        val wideBodyStart = wide.inkMinX(48 until 120)
        val narrowBodyStart = narrow.inkMinX(24 until 96)

        assertEquals(24, wideMarkerStart - narrowMarkerStart)
        assertEquals(24, wideBodyStart - narrowBodyStart)
    }

    @Test
    fun markerGutterExpandsToWholeEmForTheWidestMarker() {
        val pixels = render(document(start = 9, itemTexts = listOf("9.", "10.")), width = 600)
        val firstMarkerStart = pixels.inkMinX(0 until 48, yRange = 0 until 36)
        val firstBodyStart = pixels.inkMinX(48 until 120, yRange = 0 until 36)

        assertEquals(48, firstBodyStart - firstMarkerStart)
    }

    private fun render(document: MarkdownRenderDocument, width: Int): PixelMap =
        ImageComposeScene(width = width, height = 120) {
            Box(Modifier.fillMaxSize().background(Color.White)) {
                TiqianMarkdown(document = document, style = style)
            }
        }.use { scene ->
            scene.render(0L).toComposeImageBitmap().toPixelMap()
        }

    private fun document(start: Int, itemTexts: List<String>) = MarkdownRenderDocument(
        blocks = listOf(
            MarkdownList(
                ordered = true,
                startNumber = start,
                tight = true,
                items = itemTexts.mapIndexed { index, text ->
                    MarkdownListItem(
                        blocks = listOf(
                            MarkdownParagraph(
                                text = MarkdownText(text),
                                metadata = metadata(2 + index, listOf(0, index, 0)),
                            ),
                        ),
                        metadata = metadata(1 + index, listOf(0, index)),
                    )
                },
                metadata = metadata(0, listOf(0)),
            ),
        ),
    )

    private fun metadata(stableKey: Int, path: List<Int>) = MarkdownNodeMetadata(
        key = MarkdownNodeKey(stableKey, path),
        sourceSpan = MarkdownSourceSpan(0, 0, 0, 0, 0, 0),
    )

    private fun PixelMap.inkRows(xRange: IntRange): Set<Int> = buildSet {
        for (x in xRange) {
            for (y in 0 until height) {
                if (this@inkRows[x, y] != Color.White) add(y)
            }
        }
    }

    private fun PixelMap.inkMinX(
        xRange: IntRange,
        yRange: IntRange = 0 until height,
    ): Int = xRange.first { x -> yRange.any { y -> this[x, y] != Color.White } }

    private companion object {
        val style = MarkdownStyle(
            body = TextStyle(color = Color.Black, fontSize = 24.sp, lineHeight = 36.sp),
            blockSpacing = 0.dp,
        )
    }
}
