# Accordion

> Progressively reveal supporting content in place, using native details and summary with no JS.

- Type: Component (disclosure, Navigation)
- Namespace: `ui.accordion`
- Source: `src/ui/accordion.cljc`
- Public fns: `accordion-class-list`, `accordion-classes`, `accordion`
- Keywords: accordion, collapsible, details, faq, expand

## When to use

- Stacked sections read in order where most people only need some: FAQ, advanced settings, long help.
- Content that must collapse on server-rendered pages without JS.

## When not to use

- Use tabs for peer panels of one object where one is always visible.
- Use dialog or popover for content that should float over the page.
- Do not hide the primary content of a page in a collapsed accordion.

## Usage

```clojure
(require '[ui.accordion :as accordion])

[:div
 (accordion/accordion {:title "What is this framework?" :open true}
   [:p "A cross-target component library for Clojure."])
 (accordion/accordion {:title "How do I use it?"}
   [:p "Require the namespace and call the functions."])]
```

## API reference

### `accordion`

One collapsible item: <details> with a styled <summary> and a chevron. Stack several for a group; adjacent items join their borders.

Children: The revealed content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string` | – | Trigger text. |
| `:open` | `boolean` | – | Initially expanded. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | For example {:name "faq"} to make items exclusive in supporting browsers. |

### `accordion-classes`

Class string for the item.

### `accordion-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space | Toggles the focused item (native summary). |

## Accessibility

- Native details and summary expose the expanded state without ARIA.
- The chevron is aria-hidden; the title is the accessible name.

## Motion

- The chevron rotates and the trigger tints over --duration-fast. Height does not animate.

## Responsive behavior

- Full width of its column; the title wraps.

## Targets

- **Hiccup (`:clj`)**: Works without JS. State is browser-side only.
- **Replicant (`:cljs`)**: Uncontrolled; listen for the toggle event via :attrs if you must react.
- **Squint (`:squint`)**: Uncontrolled; never pass :open false, use (when x true).

## Notes for AI

- Titles are questions or short labels in sentence case.
- For a FAQ, keep one item open by default on first paint.

## Related

- [Tabs](tabs.md): Switch between peer panels of one object, with no JS: hidden radios drive the panels.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.

## Guidance for AI tools

Accordion: Progressively reveal supporting content in place, using native details and summary with no JS. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
