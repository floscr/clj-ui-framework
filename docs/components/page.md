# Page

> The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app.

- Type: Component (layout, Page)
- Namespace: `ui.page`
- Source: `src/ui/page.cljc`
- Public fns: `page-class-list`, `page-classes`, `page`, `page-header`, `page-section`, `action-row-class-list`, `action-row`
- Keywords: page, container, layout, max-width, gutter, page header, section, title, actions, shell

## When to use

- Every screen of an app: wrap the whole content in one page, give it one page-header, and split the rest into page-sections.
- A settings, list or form page (the default :reading width, 720px).
- A dashboard or a table-heavy page (:wide, 1200px).

## When not to use

- Use sidebar for an app shell with navigation; put a page inside its main area.
- Use card for a boundary inside a page, never as a second container around the page.
- Use dialog for a focused task that interrupts the page.

## Usage

```clojure
(require '[ui.page :as page])

(page/page {}
  (page/page-header {:title "Projects" :description "Everything your team is working on."}
    (button/button {:variant :primary :icon-left :plus} "New project"))
  (page/page-section {:title "Active"}
    (list-row/list-rows {} ...))
  (page/page-section {:title "Archived"}
    (table/table {} ...)))
```

## Examples

### Settings page: reading width, sections of settings rows

```clojure
(page/page {}
  (page/page-header {:title "Settings"})
  (page/page-section {:title "Notifications" :description "What we email you about."}
    (settings/settings-group {}
      (settings/settings-row {:label "Mentions"} (switch/switch-toggle {:checked true}))
      (settings/settings-row {:label "Weekly summary"} (switch/switch-toggle {}))))
  (page/action-row {}
    (button/button {} "Cancel")
    (button/button {:variant :primary} "Save changes")))
```

### Dashboard: wide, a metric row then a table

```clojure
(page/page {:width :wide}
  (page/page-header {:title "Overview"}
    (button-group/button-group {:variant :boxed} ...))
  (metric/metric-row {}
    (metric/metric {:label "Revenue" :value "48.2k" :unit "EUR" :delta "+12%" :trend :up})
    (metric/metric {:label "Orders" :value "1,204"}))
  (page/page-section {:title "Recent orders"}
    (table/table {} ...)))
```

## API reference

### `page`

The container: max width by :width, fluid gutters (16 to 32px), regions stacked 24px apart. Renders a <main> unless :tag says otherwise. Children never add their own gutter or max width.

Children: page-header, page-sections, cards, tables, action-row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:width` | `:narrow \| :reading \| :wide \| :full` | `:reading` | 640px for a single form or flow, 720px for settings, lists and forms, 1200px for dashboards and tables, :full for no max width. |
| `:tag` | `keyword` | `:main` | Element to render; :div when the page sits inside another main. |
| `:class` | `string` | – | Extra classes. |
| `:attrs` | `map` | – | Extra HTML attributes. |

### `page-header`

The title row: the page's only h1 (28px, medium weight) with an optional one-line description, and the page actions on the right. The one :primary button of the page goes here.

Children: The actions: buttons, a search field.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string \| hiccup` | – | The h1. |
| `:description` | `string \| hiccup` | – | One sentence under the title, muted. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `page-section`

A titled region: h2 (18px) with optional description and actions, then the content 16px apart. Consecutive sections sit 32px apart.

Children: The content: a settings-group, list-rows, a table, a card, fields.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string \| hiccup` | – | The h2. Omit for an untitled region. |
| `:description` | `string \| hiccup` | – | One sentence under the heading. |
| `:actions` | `hiccup` | – | Rendered at the right of the heading (a small button or link). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `action-row`

A row of buttons 8px apart, right-aligned so the primary action ends the row. Use it for a form's submit row at the bottom of a page or card.

Children: Buttons.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:align` | `:end \| :start \| :between` | `:end` | Where the buttons sit. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `page-classes`

Class string for {:width}.

### `page-class-list`

Vector form of page-classes.

### `action-row-class-list`

Vector of classes for {:align}.

## Accessibility

- page renders a <main>; keep one per document (pass :tag :div for a nested one).
- page-header holds the page's single h1; page-section headings are h2, so the outline reads as a document.

## Motion

- None.

## Responsive behavior

- Gutters shrink to 16px on phones, the vertical rhythm to 20px; header actions wrap under the title.
- Children get min-width 0, so long titles and wide tables cannot push the page sideways; wide tables scroll inside their card.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Start every screen with (page/page {} (page/page-header {:title ...} actions) ...). Do not write a .container, .app-shell or .app class with your own max-width, padding or font.
- One h1 per page (the page-header title), one :primary button per page (in the header or the final action-row).
- Rhythm comes from the components: 24px between regions, 16px inside a section, 32px between sections. Do not add margins to children.
- Pick :reading unless the page is a dashboard or a wide table (:wide). Never narrow a page with padding; use :narrow.
- Headings are the base ramp: do not restyle h1 or h2, and do not add eyebrow labels above them.

## Related

- [Sidebar](sidebar.md): An app shell: a sticky sidebar with brand, search, grouped menus, collapsible sections, user footer and a mobile drawer, next to the main area.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- [Settings rows](settings.md): Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen.
- [Metric](metric.md): A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record.
- [List row](list-row.md): Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.

## Guidance for AI tools

Page: The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
