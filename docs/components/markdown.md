# Markdown

> A minimal markdown to hiccup renderer for docs and chat replies: headings, code blocks, tables, lists, inline code and bold.

- Type: Component (text, Text)
- Namespace: `ui.markdown`
- Source: `src/ui/markdown.cljc`
- Public fns: `markdown->hiccup`
- Keywords: markdown, render, hiccup, docs, code block

## When to use

- Rendering documentation, help text or assistant replies written in simple markdown.

## When not to use

- Use a full parser for nested lists, links in tables, footnotes or HTML passthrough.
- Escape untrusted input yourself before rendering.

## Usage

```clojure
(require '[ui.markdown :as markdown])

(into [:div.md] (markdown/markdown->hiccup "# Title\n\nSome **bold** text with `code`."))
```

## API reference

### `markdown->hiccup`

Converts a markdown string to a vector of hiccup elements with md- classes.

## Accessibility

- Produces semantic headings, lists, tables and pre blocks.

## Motion

- None.

## Responsive behavior

- Code blocks scroll horizontally; tables are plain and may need a wrapper on phones.

## Targets

- **Hiccup (`:clj`)**: Pure function.
- **Replicant (`:cljs`)**: Pure function.
- **Squint (`:squint`)**: Pure function.

## Notes for AI

- Pass the output as children of a chat-bubble or a card body.

## Related

- [Chat](chat.md): Conversation building blocks: a message log, user and assistant bubbles, a thinking indicator, a toolbar and an auto-growing composer.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.

## Guidance for AI tools

Markdown: A minimal markdown to hiccup renderer for docs and chat replies: headings, code blocks, tables, lists, inline code and bold. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
