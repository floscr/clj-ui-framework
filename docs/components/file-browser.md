# File browser

> Grid tiles and a sortable list or table of files and folders with type icons, selection and per-item context menus.

- Type: Component (data, Collections)
- Namespace: `ui.file-browser`
- Source: `src/ui/file_browser.cljc`
- Public fns: `file-type->icon`, `file-type-icon`, `file-view-toggle`, `file-item-grid`, `col-name`, `col-size`, `col-modified`, `col-type`, `file-table`, `file-list-header`, `file-item-list`
- Keywords: file browser, file manager, folder, grid view, list view, sortable columns

## When to use

- Browsing files, folders, media or any collection that switches between a tile grid and a detail list.
- Lists that need sortable columns and a right-click menu per row.

## When not to use

- Use table for plain records without a grid view.
- Use grid for image tiles without file semantics.

## Usage

```clojure
(require '[ui.file-browser :as fb])

[:div.fb-list
 (fb/file-list-header {:sort-key :name :sort-dir :asc :on-sort sort!})
 (for [item files]
   (fb/file-item-list {:item item :on-click open!
                       :context-menu-items [{:label "Open" :icon :folder}
                                            {:label "Delete" :icon :trash :variant :danger}]}))]
```

## Examples

### Grid tiles with a view toggle

```clojure
(fb/file-view-toggle {:view :grid :on-grid-click #(set-view! :grid) :on-list-click #(set-view! :list)})
[:div.fb-grid (for [item files] (fb/file-item-grid {:item item :selected (= item current)}))]
```

### Custom table columns

```clojure
(fb/file-table {:columns [fb/col-name fb/col-size {:key :owner :label "Owner" :width "120px"}]
                :items files :sort-key :name :sort-dir :asc :on-sort sort! :on-row-click open!})
```

## API reference

### `file-item-grid`

A file as a grid tile (icon, name, size).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:item` | `{:name :file-type :size}` | – |  |
| `:selected` | `boolean` | – |  |
| `:on-click` | `fn` | – |  |
| `:context-menu-items` | `vector` | – | Wraps the tile in a context-menu-trigger. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-item-list`

A file as a list row (name, size, modified, type).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:item` | `{:name :file-type :size :modified}` | – |  |
| `:selected` | `boolean` | – |  |
| `:on-click` | `fn` | – |  |
| `:context-menu-items` | `vector` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-list-header`

Column headers for the list view with sort indicators.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:sort-key` | `:name \| :size \| :modified \| :type` | – |  |
| `:sort-dir` | `:asc \| :desc` | – |  |
| `:on-sort` | `fn [col-key]` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-table`

Configurable CSS-grid table with sortable headers, row click, selection and per-row context menus.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:columns` | `vector of {:key :label :width :render :class}` | `default-columns` |  |
| `:items` | `vector` | – |  |
| `:sort-key` | `keyword` | – |  |
| `:sort-dir` | `:asc \| :desc` | – |  |
| `:on-sort` | `fn [col-key]` | – |  |
| `:on-row-click` | `fn [item]` | – |  |
| `:selected-fn` | `fn [item] -> boolean` | – |  |
| `:context-menu-items-fn` | `fn [item] -> items` | – |  |
| `:row-attrs-fn` | `fn [item] -> map` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `file-view-toggle`

Grid and list toggle buttons.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:view` | `:grid \| :list` | – |  |
| `:on-grid-click` | `fn` | – |  |
| `:on-list-click` | `fn` | – |  |

### `file-type-icon`

Icon for a file type.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:file-type` | `keyword` | – | :folder :image :video :audio :document and so on. |
| `:size` | `icon size` | – |  |

### `file-type->icon`

Icon name for a file type.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab / Enter | Rows and tiles are buttons. |
| Shift+F10 | Opens the row's context menu. |

## Accessibility

- Sortable headers are buttons with the sort direction in text; type icons are decorative next to the name.

## Motion

- Row and tile hover tints over --duration-fast.

## Responsive behavior

- The list header and rows share a grid template; on phones prefer the grid view or hide secondary columns with :columns.

## Targets

- **Hiccup (`:clj`)**: Render links in :attrs or :row-attrs-fn for navigation; context menus work through the runtime.
- **Replicant (`:cljs`)**: All callbacks available.
- **Squint (`:squint`)**: Same as Replicant; file-type->icon returns strings.

## Notes for AI

- Sort in the host and pass the sorted :items; the component only renders indicators.

## Related

- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [Tile grid](grid.md): A responsive auto-fill grid of square or masonry tiles with size presets, plus a layout toggle and size stepper for toolbars.
- [Context menu](context-menu.md): Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element.
- [Drop zone](drop-zone.md): A generous target for dropping or picking files, with a full-page overlay variant and a click-to-add label.

## Guidance for AI tools

File browser: Grid tiles and a sortable list or table of files and folders with type icons, selection and per-item context menus. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
