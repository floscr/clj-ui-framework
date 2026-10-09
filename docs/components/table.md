# Table

> Records in columns from a headers vector and row vectors, with striped and bordered variants.

- Type: Component (data, Collections)
- Namespace: `ui.table`
- Source: `src/ui/table.cljc`
- Public fns: `table-class-list`, `table-classes`, `table`
- Keywords: table, data table, rows, columns, records

## When to use

- Structured records with the same fields per row that people compare across columns.
- Admin and settings lists with a handful of columns.

## When not to use

- Use card-list for one-column lists of options or settings rows.
- Use file-browser for files and folders with grid and list views.
- For sortable or filterable data, add controls above the table; sorting is not built in yet.

## Usage

```clojure
(require '[ui.table :as table])

(table/table {:variant :striped
              :headers ["Name" "Email" "Role"]
              :rows [["Alice Johnson" "alice@example.com" (badge/badge {:variant :success} "Active")]
                     ["Bob Smith" "bob@example.com" "Editor"]]})
```

## Examples

### Bordered, inside a card

```clojure
(card/card {:class "card-flush"}
  (table/table {:variant :bordered :headers ["Key" "Value"] :rows rows}))
```

## API reference

### `table`

Renders <table> inside a .table-wrapper that scrolls horizontally when the columns overflow.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:headers` | `vector` | – | Column header cells (strings or hiccup). |
| `:rows` | `vector of vectors` | – | Each row is a vector of cell values (strings or hiccup). |
| `:variant` | `nil \| :striped \| :bordered` | – | Zebra rows, or a bordered rounded frame. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `table-classes`

Class string for {:variant}.

### `table-class-list`

Vector form.

## Accessibility

- Real <table>, <thead> and <th> markup, so screen readers get column headers.
- Put status in a badge with a word, never color alone.

## Motion

- Row hover tint over --duration-fast.

## Responsive behavior

- Wide tables scroll inside the wrapper, not the page. On phones consider a card-list per record instead.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Numbers that align in a column get class tabular-nums and right alignment.
- Headers are sentence case.

## Related

- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- `file-browser`: File browser components — grid/list views.
- [Pagination](pagination.md): Move through a long collection with numbered pages and previous and next links.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.

## Guidance for AI tools

Table: Records in columns from a headers vector and row vectors, with striped and bordered variants. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
