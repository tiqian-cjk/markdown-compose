package org.tiqian.markdown

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class MarkdownFootnoteNavigationState(
    private val coroutineScope: CoroutineScope,
) {
    private val definitions = mutableMapOf<String, BringIntoViewRequester>()
    private val references = mutableMapOf<String, MutableList<BringIntoViewRequester>>()
    private val activeReferences = mutableMapOf<String, BringIntoViewRequester>()

    fun registerDefinition(label: String, requester: BringIntoViewRequester) {
        definitions[label] = requester
    }

    fun unregisterDefinition(label: String, requester: BringIntoViewRequester) {
        if (definitions[label] === requester) definitions.remove(label)
    }

    fun registerReference(label: String, requester: BringIntoViewRequester) {
        val requesters = references.getOrPut(label) { mutableListOf() }
        if (requesters.none { it === requester }) requesters += requester
    }

    fun unregisterReference(label: String, requester: BringIntoViewRequester) {
        references[label]?.let { requesters ->
            requesters.removeAll { it === requester }
            if (requesters.isEmpty()) references.remove(label)
        }
        if (activeReferences[label] === requester) activeReferences.remove(label)
    }

    fun bringDefinitionIntoView(label: String, reference: BringIntoViewRequester) {
        activeReferences[label] = reference
        coroutineScope.launch { definitions[label]?.bringIntoView() }
    }

    fun bringReferenceIntoView(label: String) {
        coroutineScope.launch {
            (activeReferences[label] ?: references[label]?.firstOrNull())?.bringIntoView()
        }
    }
}

internal val LocalMarkdownFootnoteNavigationState =
    compositionLocalOf<MarkdownFootnoteNavigationState?> { null }

@Composable
internal fun rememberMarkdownFootnoteNavigationState(): MarkdownFootnoteNavigationState {
    val coroutineScope = rememberCoroutineScope()
    return remember(coroutineScope) { MarkdownFootnoteNavigationState(coroutineScope) }
}
