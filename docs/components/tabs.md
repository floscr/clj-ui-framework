# Tabs

> Switch between peer panels of one object, with no JS: hidden radios drive the panels.

- Type: Component (disclosure, Navigation)
- Namespace: `ui.tabs`
- Source: `src/ui/tabs.cljc`
- Public fns: `tabs-class-list`, `tabs-classes`, `tabs`
- Keywords: tabs, tab panel, segmented, underline tabs, css only

## When to use

- Peer panels of one object: Overview, Activity, Settings.
- Content that should switch in place without a route change, on all targets including server-rendered hiccup.

## When not to use

- Use button-group for two to five views of the same data (a range or a layout toggle).
- Use accordion for stacked sections that are read in order.
- Use real links when each panel is its own page.

## Usage

```clojure
(require '[ui.tabs :as tabs])

(tabs/tabs {:variant :line
            :default "account"
            :tabs [{:id "account"  :label "Account"  :content [:div "..."]}
                   {:id "password" :label "Password" :content [:div "..."]}]})
```

## Examples

### Boxed segmented tabs with a fixed radio name

```clojure
(tabs/tabs {:name "view"
            :tabs [{:id "grid" :label "Grid" :content grid}
                   {:id "list" :label "List" :content list}]})
```

## API reference

### `tabs`

Renders a radio group of triggers and the matching panels. The active tab is uncontrolled; the browser tracks it.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:boxed \| :line` | `:boxed` | :boxed is a segmented control with a raised pill; :line is underlined tabs. |
| `:default` | `string \| keyword` | – | Id of the initially active tab; defaults to the first. |
| `:tabs` | `vector of {:id :label :content}` | – | The tabs. :content is hiccup. |
| `:name` | `string` | – | Radio group name. Auto-generated when omitted; pass one for deterministic markup (tests, SSR snapshots). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | Attributes on the root element. |

### `tabs-classes`

Class string for {:variant}.

### `tabs-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Arrow keys | Move between tabs (native radio behavior). |
| Tab | Moves focus into and out of the tab list. |

## Accessibility

- Triggers are labels for radio inputs; the focus ring is drawn on the label of the focused radio.
- Panels are hidden with CSS :has(), so hidden content is not read.

## Motion

- Trigger color and background change over --duration-fast; no sliding indicator.

## Responsive behavior

- The tab list wraps; keep labels short. For many tabs prefer a select on phones.

## Targets

- **Hiccup (`:clj`)**: Works without JS. State is not observable server-side.
- **Replicant (`:cljs`)**: Uncontrolled: to react to the active tab, listen for change on the radios via :attrs or render your own triggers.
- **Squint (`:squint`)**: Same as Replicant; pass :name to keep ids stable across re-renders.

## Notes for AI

- Tabs need no state; do not add an active-tab atom unless you must react to changes.
- For a range toggle (7d, 30d, 90d) use button-group, not tabs.

## Related

- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Accordion](accordion.md): Progressively reveal supporting content in place, using native details and summary with no JS.
- [Toolbar](toolbar.md): A pill-shaped container that groups icon buttons with separators, horizontal or vertical, optionally floating in a popover.

## Guidance for AI tools

Tabs: Switch between peer panels of one object, with no JS: hidden radios drive the panels. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
