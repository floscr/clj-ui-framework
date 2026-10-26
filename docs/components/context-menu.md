# Context menu

> Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element.

- Type: Component (actions, Menus)
- Namespace: `ui.context-menu`
- Source: `src/ui/context_menu.cljc`
- Public fns: `icon-paths-for`, `normalize-item`, `items->json`, `context-menu-trigger`
- JS runtime: `ui.js.context-menu`, `ui.js.gestures` (bundled in ui-runtime.js)
- Keywords: context menu, right click, long press, actions menu, row actions

## When to use

- Secondary actions on a specific object: a file, a row, a tile, a message.
- Actions that should stay out of the way until asked for, on desktop and touch alike.

## When not to use

- Use command for global actions and search.
- Use popover for a panel with controls, not a list of actions.
- Put primary actions in visible buttons; a menu must never be the only way to reach an important action.

## Usage

```clojure
(require '[ui.context-menu :as context-menu])

(context-menu/context-menu-trigger
  {:items [{:label "Edit" :url "/edit/123" :icon :edit}
           {:type :separator}
           {:label "Delete" :url "/del/123" :icon :trash :variant :danger :confirm true}]}
  [:div.file-tile "report.pdf"])
```

## Examples

### Callbacks in Replicant or Squint

```clojure
(context-menu/context-menu-trigger
  {:items [{:label "Rename" :icon :edit :on-click #(rename! id)}
           {:label "Delete" :icon :trash :variant :danger
            :confirm "Delete this file permanently?" :on-click #(delete! id)}]}
  tile)
```

## API reference

### `context-menu-trigger`

Wraps children in a trigger element. The menu itself is built by the JS runtime on contextmenu.

Children: The element that owns the actions.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:items` | `vector of item maps` | – | {:label :url :icon :variant :confirm :confirm-style :confirm-title :on-click} or {:type :separator}. :variant :danger tints the row. :confirm true (or a string) asks before running: by default the row arms in place ("Tap again to delete", 3s countdown) and the second activation runs it. :confirm-style :panel drills into a confirm page instead, showing :confirm-title, the :confirm string as the message, the action and Cancel. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `normalize-item`

Flattens an item map for the runtime (adds :icon-paths, stringifies keywords).

### `icon-paths-for`

SVG child elements for an icon name, for the runtime to draw without the icon module.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Shift+F10 / Menu key | Opens the menu on the focused trigger (native contextmenu). |
| ArrowUp / ArrowDown | Move between items. |
| Enter | Runs the item; on a :confirm item the first press arms it, the second runs it. |
| Escape | Disarms an armed item, steps back from a confirm panel, otherwise closes. |

## Accessibility

- Items are real buttons or links inside the runtime menu; disabled items use aria-disabled.
- Long-press on touch is handled globally by the gestures runtime; the pressed element shows press feedback.
- While the menu is open, text selection is off for the whole page (.clj-ui-menu-open on html) and any further right-click only closes the menu; neither another menu nor the browser menu opens.

## Motion

- The menu scales in from its corner over --duration-fast with --ease-enter and is clamped to the viewport.
- An armed row pops in with --ease-spring and drains a countdown bar; the confirm panel slides in while the menu height animates (skipped under reduced motion).

## Responsive behavior

- On touch, a 500ms long-press opens the menu; the iOS callout is suppressed on the trigger.

## Targets

- **Hiccup (`:clj`)**: Items are serialized into a data attribute; :url drives navigation.
- **Replicant (`:cljs`)**: Items are passed to the runtime directly; :on-click works.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- Add :data-long-press to any element with a custom contextmenu handler so touch users get the same menu.
- Do not add your own selection or right-click guards while the menu is open; the runtime toggles .clj-ui-menu-open on html and swallows contextmenu events itself.
- Destructive items: :variant :danger plus :confirm; label with verb and object (the armed row reads "Tap again to <label>").
- Use :confirm-style :panel when the user should see what is affected (pass :confirm-title and a :confirm message); keep the default for quick, low-stakes deletes.

## Related

- [Command palette](command.md): A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select.
- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Button](button.md): A single action with quiet secondary states, press feedback and a loading spinner.

## Guidance for AI tools

Context menu: Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
