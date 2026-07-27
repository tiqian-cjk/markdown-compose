# Architecture

The renderer has three boundaries:

1. `markdown-parser` owns parsing and incremental AST production.
2. `MarkdownCompiler` lowers that AST into the renderer-owned `MarkdownRenderDocument`.
3. `TiqianMarkdown` renders prose with Tiqian and dispatches non-prose blocks through Compose slots.

The render model contains no Compose or parser nodes. It keeps source spans, stable keys, inline
semantics, and explicit capability issues so unsupported syntax is observable rather than silently
dropped.

`TiqianMarkdown` owns neither scrolling nor network image loading. Hosts compose it into their own
scroll container and replace code, image, math, HTML, table, footnote, custom, thematic-break, or
unsupported-block slots as needed. Host-only blocks are identified by pure-data
`MarkdownCustomBlock` values; their parser nodes and Compose content stay in the host adapter.

Text falls back block-by-block to Compose text when Tiqian cannot preserve the lowered semantics.
`MarkdownTextFallbackPolicy.Disabled` keeps a no-fallback dogfood mode.
