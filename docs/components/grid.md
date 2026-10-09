# Tile grid

> A responsive auto-fill grid of square or masonry tiles with size presets, plus a layout toggle and size stepper for toolbars.

- Type: Component (layout, Layout)
- Namespace: `ui.grid`
- Source: `src/ui/grid.cljc`
- Public fns: `grid-class-list`, `grid-classes`, `grid`, `layout-toggle`, `step-size`, `size-stepper`
- JS runtime: `ui.js.masonry` (bundled in ui-runtime.js)
- Keywords: grid, masonry, tiles, photo grid, gallery, tile size

## When to use

- Photo and media galleries, cards of equal weight, any tile collection that should reflow by width.
- Letting people pick tile size and layout from a toolbar.

## When not to use

- Use file-browser for files with names, sizes and list views.
- Use the layout.css 12-column helpers for page layout, not for collections.

## Usage

```clojure
(require '[ui.grid :as grid])

(grid/grid {:layout :masonry :size :m}
  (for [img images] (lightbox/image-thumbnail {:src (:src img)})))
```

## Examples

### Toolbar controls

```clojure
(grid/layout-toggle {:value layout :on-change set-layout! :boxed true})
(grid/size-stepper {:value size :on-change set-size!})
```

### Server-rendered toggle with links

```clojure
(grid/layout-toggle {:value "grid" :options [{:value "grid" :icon :grid :label "Grid" :href "/?view=grid"}
                                            {:value "masonry" :icon :layers :label "Masonry" :href "/?view=masonry"}]})
```

## API reference

### `grid`

The grid container. Masonry uses the runtime where the browser lacks native masonry rows.

Children: Tiles.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:layout` | `:grid \| :masonry` | `:grid` |  |
| `:size` | `:s \| :m \| :l \| :xl \| :xxl` | `:m` | Tile minimum width preset. |
| `:min` | `CSS length` | – | Explicit tile min width; wins over :size. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `layout-toggle`

Segmented icon toggle between layouts.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `keyword \| string` | – |  |
| `:options` | `vector of {:value :icon :label :href}` | `grid and masonry` |  |
| `:on-change` | `fn [value]` | – |  |
| `:boxed` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `size-stepper`

Minus and plus stepper over the size presets, disabled at the ends.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `keyword \| string` | – |  |
| `:sizes` | `vector` | `ui.grid/sizes` |  |
| `:on-change` | `fn [size]` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `step-size`

Pure: next size along a vector, clamped.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the toggle and stepper buttons. |

## Accessibility

- Toggle options have labels; the stepper buttons are labeled and disabled at the ends.

## Motion

- None on the grid; masonry re-layout happens on resize without animation.

## Responsive behavior

- Auto-fill reflows by width; pick :s on phones through the stepper.

## Targets

- **Hiccup (`:clj`)**: Give toggle options :href; the masonry runtime self-initializes.
- **Replicant (`:cljs`)**: Use :on-change on both controls.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Tiles decide their own aspect ratio; the grid only sets columns.

## Related

- [Lightbox](lightbox.md): A fullscreen image overlay with pinch, wheel and double-tap zoom and drag to pan, plus a clickable thumbnail.
- [File browser](file-browser.md): Grid tiles and a sortable list or table of files and folders with type icons, selection and per-item context menus.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.

## Guidance for AI tools

Tile grid: A responsive auto-fill grid of square or masonry tiles with size presets, plus a layout toggle and size stepper for toolbars. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
