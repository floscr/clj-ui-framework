# Command palette

> A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select.

- Type: Component (actions, Menus)
- Namespace: `ui.command`
- Source: `src/ui/command.cljc`
- Public fns: `command-item`, `command-group`, `command-dialog`, `command-trigger`
- JS runtime: `ui.js.command` (bundled in ui-runtime.js)
- Keywords: command palette, cmdk, search commands, keyboard, hotkey, quick switcher

## When to use

- Global search and actions behind one hotkey (mod+k): navigate, create, switch.
- A long list that needs filtering and keyboard selection, such as a file or page switcher.

## When not to use

- Use select for one of many values in a form.
- Use context-menu for actions on a specific object under the pointer.
- Use form search-bar for a search that submits and shows results on a page.

## Usage

```clojure
(require '[ui.command :as command])

(command/command-trigger {:target "cmdk" :class (button/button-classes {})} "Search…")

(command/command-dialog {:id "cmdk" :placeholder "Type a command or search…" :hotkey "mod+k"}
  (command/command-group {:heading "Pages"}
    (command/command-item {:icon :calendar :url "/calendar" :shortcut "⌘P"} "Calendar")
    (command/command-item {:icon :settings :url "/settings"} "Settings")))
```

## Examples

### Recents and search-only rows

```clojure
(command/command-group {:heading "Recent"}
  (command/command-item {:value "" :url "/p/42"} "Harbour redesign"))
(command/command-group {:heading "Archive"}
  (command/command-item {:search-only true :url "/p/7" :description "Archived 2025"} "Old site"))
```

### Alt quick-select with letters

```clojure
(command/command-dialog {:id "cmdk" :hotkey "mod+k" :quick-nav :letters} ...)
```

## API reference

### `command-dialog`

The palette: a native <dialog> with a search input and the item list. The runtime filters, navigates and selects.

Children: command-group and command-item forms.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – | Matched by command-trigger :target (required). |
| `:placeholder` | `string` | `"Type a command or search…"` |  |
| `:hotkey` | `string` | – | Global open shortcut, e.g. "mod+k" (mod is ⌘ on Mac, Ctrl elsewhere). |
| `:quick-nav` | `:letters \| :numbers \| string` | – | Hold Alt to show key badges on the visible rows; Alt+key selects. Off by default. |
| `:empty` | `string` | `"No results found."` | Empty-state text. |
| `:leading` | `hiccup` | – | Replaces the search icon, e.g. a back button. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `command-group`

Groups items under an optional heading. While searching, groups flatten into one ranked list.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:heading` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `command-item`

One row: a <button>, or an <a> when :url is set. Selection closes the dialog.

Children: The label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:icon` | `icon keyword` | – |  |
| `:shortcut` | `string` | – | Shortcut hint shown as a kbd, e.g. "⌘P". |
| `:description` | `string` | – | Secondary line under the label. |
| `:url` | `string` | – | Navigate on select. |
| `:value` | `string` | – | Explicit search text. An empty string shows the row only while the input is empty (recents). |
| `:search-only` | `boolean` | – | Hide while the input is empty; surface only as a search result. |
| `:on-click` | `fn` | – | Selection callback (:cljs and :squint). |
| `:disabled` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `command-trigger`

A <button> that opens the matching dialog.

Children: The trigger label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:target` | `string` | – | Id of the command-dialog (required). |
| `:class` | `string` | – | Usually a button class string. |
| `:attrs` | `map` | – |  |

## Keyboard interactions

| Keys | Action |
| --- | --- |
| mod+k (or :hotkey) | Opens the palette. |
| Type | Filters items live; groups flatten into one ranked list. |
| ArrowUp / ArrowDown, Ctrl+j / Ctrl+k | Move the highlighted row. |
| Enter | Selects the highlighted row. |
| Alt+key | Selects the badged row when :quick-nav is on. |
| Escape | Closes. |

## Accessibility

- The dialog is a native <dialog> (focus trap, Escape); items have role=option inside role=group.
- The trigger carries aria-haspopup=dialog.

## Motion

- The dialog fades and scales in over --duration-fast with --ease-enter.

## Responsive behavior

- The dialog is capped to the viewport; on phones it fills the width and the list scrolls.

## Targets

- **Hiccup (`:clj`)**: Static markup plus ui-runtime.js; items navigate via :url.
- **Replicant (`:cljs`)**: Items may use :on-click; the dialog stays uncontrolled.
- **Squint (`:squint`)**: Same as Replicant. The runtime re-filters when rows are patched in place.

## Notes for AI

- One palette per app, opened from a trigger in the header and the hotkey.
- Give every item a verb or a destination as its label; use :description for context, not for a second label.

## Related

- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Context menu](context-menu.md): Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element.
- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Command palette: A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
