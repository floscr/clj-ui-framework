# Card

> A bordered surface that groups related content and actions, plus a divided list variant.

- Type: Component (data, Surfaces)
- Namespace: `ui.card`
- Source: `src/ui/card.cljc`
- Public fns: `card-class-list`, `card-classes`, `card`, `card-header`, `card-body`, `card-footer`, `card-list-class-list`, `card-list-classes`, `card-list`, `card-list-item`
- Keywords: card, panel, surface, list, settings rows, section

## When to use

- Grouping a title, body and actions that belong together (a form section, a summary).
- A list of rows on one surface with hairline dividers (card-list), for settings or options.
- A real boundary between regions of a page. Prefer proximity and spacing first, a card second.

## When not to use

- Do not nest cards inside cards; use card-list rows or plain spacing inside a card.
- Use dialog for content that must interrupt, popover for a small anchored panel.
- Use table for records with columns.

## Usage

```clojure
(require '[ui.card :as card])

(card/card {}
  (card/card-header {} [:h3 "Card title"] [:p "Description"])
  (card/card-body {} "Content")
  (card/card-footer {}
    (button/button {} "Cancel")
    (button/button {:variant :primary} "Save changes")))
```

## Examples

### Divided list with inset dividers

```clojure
(card/card-list {:divider :inset}
  (card/card-list-item {} "Notifications")
  (card/card-list-item {} "Privacy")
  (card/card-list-item {} "Appearance"))
```

### Flush card with sections

```clojure
(card/card {:class "card-flush"}
  [:div.card-section "Header row"]
  [:div.card-section "Body row"])
```

## API reference

### `card`

The surface: bg-1 background, border-0, radius-surface, no shadow. Add class card-flush to remove padding and use .card-section children with dividers.

Children: Any hiccup; usually card-header, card-body, card-footer.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – | Extra classes (card-flush for a padding-less card). |
| `:attrs` | `map` | – | Extra HTML attributes. |

### `card-header`

Title block; headings inside lose their margin, a <p> renders as the muted description.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `card-body`

Main content; first and last child margins are trimmed.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `card-footer`

Right-aligned action row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `card-list`

A card surface whose children are rows separated by hairline dividers.

Children: card-list-item elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:divider` | `:full \| :inset` | `:full` | Edge-to-edge dividers or dividers inset from the sides. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `card-list-item`

One row of a card-list.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `card-classes`

Class string for a card.

### `card-list-classes`

Class string for {:divider}.

## Accessibility

- A card is a <div>; add a heading inside card-header so the region has a name.
- Interactive rows in a card-list should be real buttons or links, not clickable divs.

## Motion

- None.

## Responsive behavior

- Cards fill their column; the page sets gutters. Side-by-side cards share the same top and height (align-items stretch).
- Wide tables inside a card scroll inside the card, not the page.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Nested radii are concentric: inner = outer minus padding. Use calc(var(--radius-surface) - var(--size-6)) for media inside a padded card.
- Settings pages use settings rows (ui.settings), record lists use list rows (ui.list-row); card-list is the generic divided surface under both.

## Related

- [Page](page.md): The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app.
- [Settings rows](settings.md): Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen.
- [List row](list-row.md): Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere.
- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.
- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [Accordion](accordion.md): Progressively reveal supporting content in place, using native details and summary with no JS.

## Guidance for AI tools

Card: A bordered surface that groups related content and actions, plus a divided list variant. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
