# Breadcrumb

> Shows where a page sits in a hierarchy; every item but the last is a link.

- Type: Component (disclosure, Navigation)
- Namespace: `ui.breadcrumb`
- Source: `src/ui/breadcrumb.cljc`
- Public fns: `breadcrumb`
- Keywords: breadcrumb, hierarchy, navigation path, ancestors

## When to use

- Pages nested two or more levels deep: Projects, Harbour, Settings.
- Record pages where people jump back up a level often.

## When not to use

- Top-level sections belong in the sidebar or header, not a breadcrumb.
- Use pagination for sequential pages of one collection.

## Usage

```clojure
(require '[ui.breadcrumb :as breadcrumb])

(breadcrumb/breadcrumb {:items [{:label "Projects" :href "/projects"}
                                {:label "Harbour" :href "/projects/harbour"}
                                {:label "Settings"}]})
```

## API reference

### `breadcrumb`

Renders <nav aria-label=Breadcrumb> with an ordered list. The last item is the current page and is not a link.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:items` | `vector of {:label :href}` | – | Ancestors first; the last item needs no :href. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Attributes on the <ol>. |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the links. |

## Accessibility

- Landmark nav with a label; the current item is plain text.
- Add {:aria-current "page"} on the last item via :attrs on the list if your app relies on it.

## Motion

- None.

## Responsive behavior

- Below about 560px hide the root item or truncate long labels; the list does not wrap by default.

## Targets

- **Hiccup (`:clj`)**: Static links.
- **Replicant (`:cljs`)**: Static links; use your router's href scheme.
- **Squint (`:squint`)**: Static links.

## Notes for AI

- Labels mirror the page titles exactly.
- Three to four levels at most; collapse deeper paths.

## Related

- [Pagination](pagination.md): Move through a long collection with numbered pages and previous and next links.
- [Sidebar](sidebar.md): An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.
- [Tabs](tabs.md): Switch between peer panels of one object, with no JS: hidden radios drive the panels.

## Guidance for AI tools

Breadcrumb: Shows where a page sits in a hierarchy; every item but the last is a link. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
