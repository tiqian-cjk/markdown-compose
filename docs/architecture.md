# Architecture

The renderer has two boundaries:

1. The host lowers its parser AST into the renderer-owned `MarkdownRenderDocument`.
2. `TiqianMarkdown` renders prose with Tiqian and dispatches non-prose blocks and inline objects
   through Compose slots.

The module has no parser dependency. Its render model contains no Compose or parser nodes. It keeps source spans, stable keys, inline
semantics, and explicit capability issues so unsupported syntax is observable rather than silently
dropped.

`TiqianMarkdown` owns neither scrolling nor network image loading. Hosts compose it into their own
scroll container and replace code, image, HTML, table, footnote, custom, thematic-break, or
unsupported-block slots as needed. The library owns the default math boundary and ships a pinned
Lete Sans Math OpenType backend; hosts select another `MarkdownMathFont` or replace the math slots without taking a
dependency on that backend. Inline image and math slots receive pure-data marks plus the current
text style. Measured inline math keeps the original TeX source range and lowers its exact
advance/ascent/descent into `CjkInlineObject`; Tiqian therefore owns its line break, line-box expansion, and final baseline
placement instead of delegating vertical alignment to a Compose placeholder. Host-only block
and inline semantics are identified by pure-data `MarkdownCustomBlock` and `MarkdownTextMark.Custom`
values; parser nodes and Compose content stay in the host adapter.

The formula renderer, rather than this Markdown module, derives source-contiguous fragments from
its own AST and retains the measured layout used to paint each one. It exposes real line breaks
after binary and relation operators on the main formula baseline, including those inside ordinary
or automatic delimiters; fractions, roots, scripts, matrices and other stacked structures remain
atomic. Additional fragment boundaries expose measured spacing without inventing line breaks.
When a post-operator break is chosen, the operator remains on the preceding line while its measured
following math space is discarded as line-edge glue. The same space remains present when no break
is taken, and the next fragment never receives a matching leading blank.

Tiqian first stretches the renderer-measured space after punctuation, then both sides of relation
operators, then both sides of binary operators. Each edge reports its measured natural blank and an
absolute `0.5em` preferred target. Once those preferred resources reach that target, the same edges
still join the final uniform spacing pass. The math blank already measured at an internal boundary
may be removed only as the last compression tier; formula glyphs are never scaled. Chinese and ASCII
point marks following the last formula fragment remain covered by Tiqian kinsoku and cannot start an
automatically wrapped line. If the host adapter retained a Markdown separator space between the
formula and the mark, Tiqian preserves its source range but collapses its layout advance to zero and
keeps the whole formula-space-mark sequence together.

Text falls back block-by-block to Compose text when Tiqian cannot preserve the lowered semantics.
Inline image/math providers that expose advance, ascent and descent lower to Tiqian-native inline
objects, while host decorations and actions consume the same `LayoutResult` used to paint text.
They do not switch the containing paragraph to a second `BasicText` layout. Only an unmeasured
inline object or another reported capability issue uses the block-local Compose fallback; custom
dashed decoration becomes a native solid underline on that fallback path because Compose does not
expose its underline metric to an external dashed-stroke renderer.
`MarkdownTextFallbackPolicy.Disabled` keeps a no-fallback dogfood mode.
