# List row

> Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere.

- Type: Component (data, Lists)
- Namespace: `ui.list-row`
- Source: `src/ui/list_row.cljc`
- Public fns: `list-row-class-list`, `list-row-classes`, `list-rows`, `list-row`
- Keywords: list, row, item, record, playlist, inbox, thumbnail, navigation list, saved items

## When to use

- A list of records with a name and one line of detail that open a detail view (playlists, documents, devices).
- Saved items with a thumbnail, a price or date, and one or two actions per row.
- A short navigation list inside a page (categories, folders).

## When not to use

- Use table for records with several columns to compare or sort.
- Use settings rows when every row is a label with a control and nothing opens.
- Use file-browser for files and folders with selection and context menus.
- Use tile grid for images that should be seen, not read.

## Usage

```clojure
(require '[ui.list-row :as list-row])

(list-row/list-rows {}
  (list-row/list-row {:icon :music :title "Favorites" :meta "21" :href "/playlists/favorites"})
  (list-row/list-row {:icon :music :title "Recently played" :meta "50" :href "/playlists/recent"})
  (list-row/list-row {:icon :folder :title "All music" :meta "1262" :href "/library"}))
```

## Examples

### Saved items with a thumbnail and actions

```clojure
(list-row/list-row {:thumb "/img/switch.jpg"
                    :title "Super Mario Bros. Wonder"
                    :description "38 EUR · Like new · 15 Sep"}
  (button/button {:variant :ghost :size :sm :icon :edit :attrs {:aria-label "Edit"}})
  (button/button {:variant :primary :size :sm} "Publish"))
```

### Selected row

```clojure
(list-row/list-row {:title "Night drive" :description "Deep house" :active true :on-click select!})
```

## API reference

### `list-rows`

The surface (bg-1, border-0, radius-surface) that holds rows and divides them with hairlines; role list.

Children: list-row elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `list-row`

One record, 48px tall (56px with a description). The row is a <div>; with :href the title is an <a>, with :on-click a <button>, stretched over the row so the whole row opens the record while trailing buttons stay separate. Interactive rows fill on hover; the active row is tinted with the accent.

Children: Trailing controls: small icon buttons, a badge, a switch.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string \| hiccup` | – | The name, one line, truncated. |
| `:description` | `string \| hiccup` | – | Second line, muted, truncated. |
| `:icon` | `icon keyword` | – | Leading icon in a 40px slot. |
| `:thumb` | `string` | – | Leading image URL, 40px square with a small radius. Wins over :icon. |
| `:meta` | `string \| hiccup` | – | Trailing muted text: a count, a size, a date. |
| `:href` | `string` | – | The row opens this link (the title is the anchor). |
| `:on-click` | `fn` | – | The row is a button on :cljs and :squint; ignored on :clj. |
| `:active` | `boolean` | – | The selected row (aria-current). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `list-row-classes`

Class string for {:interactive :active}.

### `list-row-class-list`

Vector form of list-row-classes.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves between interactive rows and their trailing controls. |
| Enter / Space | Opens a row that is a link or a button. |

## Accessibility

- The title of a row that leads somewhere is a real link or button, so it is focusable and announced; keyboard focus rings the whole row.
- Trailing controls are ordinary buttons above the stretched title, so they are valid HTML and keyboard reachable. Thumbnails are decorative (empty alt).

## Motion

- Hover and press change the fill only, over duration-fast.

## Responsive behavior

- Title and description truncate; trailing controls keep their width. Rows are at least 48px tall, the tap minimum with the row's padding.

## Targets

- **Hiccup (`:clj`)**: Static markup; use :href or HTMX attributes in :attrs.
- **Replicant (`:cljs`)**: :on-click becomes {:on {:click f}}; :class may be a vector.
- **Squint (`:squint`)**: :on-click is a flat attribute.

## Notes for AI

- A list page is page-header, then list-rows inside a page-section; one list-rows per group, not a card per row.
- Keep the row's actions to one or two small ghost buttons; the primary action of the page lives in the page-header.
- Put counts and dates in :meta, not in the title.

## Related

- [Page](page.md): The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [File browser](file-browser.md): Grid tiles and a sortable list or table of files and folders with type icons, selection and per-item context menus.
- [Settings rows](settings.md): Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.

## Guidance for AI tools

List row: Records as rows on one surface: a leading icon or thumbnail, title and description, metadata and controls at the end, with hover for rows that lead somewhere. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
