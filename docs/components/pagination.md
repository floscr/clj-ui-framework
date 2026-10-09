# Pagination

> Move through a long collection with numbered pages and previous and next links.

- Type: Component (data, Collections)
- Namespace: `ui.pagination`
- Source: `src/ui/pagination.cljc`
- Public fns: `pagination-item-class-list`, `pagination-item-classes`, `pagination`
- Keywords: pagination, pages, next previous, paging

## When to use

- Collections split into pages on the server, where each page has a URL.
- Tables and lists with a known total page count.

## When not to use

- Use infinite scroll or a Load more button for feeds without a meaningful page number.
- Use tabs or button-group to switch between views, not pages.

## Usage

```clojure
(require '[ui.pagination :as pagination])

(pagination/pagination {:current 3 :total 10
                        :href-fn #(str "/items?page=" %)})
```

## Examples

### Client-side paging

```clojure
(pagination/pagination {:current page :total 10 :on-click set-page!})
```

## API reference

### `pagination`

Renders <nav aria-label=Pagination> with Previous, every page number and Next.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:current` | `int` | – | Current page, 1-based. |
| `:total` | `int` | – | Total number of pages. |
| `:href-fn` | `fn [page] -> url` | – | Builds each page link (hiccup). |
| `:on-click` | `fn [page]` | – | Click handler on :cljs and :squint; prevents the default navigation. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `pagination-item-classes`

Class string for {:active :disabled}.

### `pagination-item-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the page links. |
| Enter | Follows the link. |

## Accessibility

- Links inside a labeled nav; the active page is marked by its class and should also carry aria-current via :attrs when needed.
- Disabled Previous and Next keep their href but are styled inert.

## Motion

- Hover tint over --duration-fast.

## Responsive behavior

- Every page number renders; for more than about ten pages show a window of pages in the app before passing :total.

## Targets

- **Hiccup (`:clj`)**: Use :href-fn; each page is a real link.
- **Replicant (`:cljs`)**: Use :on-click for client-side paging; :href-fn is optional.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Numbers use tabular-nums; keep Previous and Next as words, not arrows alone.

## Related

- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [Breadcrumb](breadcrumb.md): Shows where a page sits in a hierarchy; every item but the last is a link.
- `button-group`: Button group — a cluster of related buttons rendered as a segmented

## Guidance for AI tools

Pagination: Move through a long collection with numbered pages and previous and next links. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
