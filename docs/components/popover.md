# Popover

> A small anchored panel for contextual controls, built on the native Popover API so it works without app state.

- Type: Component (disclosure, Overlays)
- Namespace: `ui.popover`
- Source: `src/ui/popover.cljc`
- Public fns: `trigger-attrs`, `content-class-list`, `popover-trigger`, `popover-content`, `popover-header`, `popover-title`, `popover-description`
- JS runtime: `ui.js.popover` (bundled in ui-runtime.js)
- Keywords: popover, anchored panel, popovertarget, dropdown panel, settings panel

## When to use

- Small anchored controls (a couple of fields, a picker) that should not dim the page.
- Contextual information next to its trigger.

## When not to use

- Use dialog for a decision that must interrupt.
- Use context-menu for a list of actions on right-click or long press.
- Use tooltip for one line of hint text.

## Usage

```clojure
(require '[ui.popover :as popover])

(popover/popover-trigger {:target "dimensions"
                          :class (button/button-classes {:variant :secondary})}
  "Open")
(popover/popover-content {:id "dimensions" :side :bottom :align :start}
  (popover/popover-header {}
    (popover/popover-title {} "Dimensions")
    (popover/popover-description {} "Set the dimensions for the layer."))
  (form/form-field {:label "Width"} (form/form-input {:value "100%"})))
```

## Examples

### Turn an existing button into a trigger

```clojure
(button/button {:variant :ghost :icon :settings
                :attrs (merge {:aria-label "Settings"} (popover/trigger-attrs "settings"))})
```

## API reference

### `popover-trigger`

A <button> with popovertarget pointing at the content's id.

Children: The trigger label.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:target` | `string` | – | Id of the matching popover-content (required). |
| `:class` | `string` | – | Usually a button class string. |
| `:attrs` | `map` | – |  |

### `popover-content`

The floating panel (popover="auto"). The JS runtime anchors it to the trigger.

Children: popover-header and any content.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:id` | `string` | – | Must match the trigger's :target (required). |
| `:side` | `:top \| :right \| :bottom \| :left` | `:bottom` | Which side of the trigger the panel opens on. |
| `:align` | `:start \| :center \| :end` | `:center` | Alignment along that side. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `popover-header`

Title block.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `popover-title`

Heading text.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `popover-description`

Muted description.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `trigger-attrs`

Attribute map {:popovertarget id :aria-haspopup "dialog"} to spread onto any element.

### `content-class-list`

Class vector for {:side}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Escape | Closes the popover (native). |
| Enter / Space | On the trigger, toggles it. |

## Accessibility

- The trigger carries aria-haspopup=dialog; the native Popover API manages light dismiss and Escape.
- Give the panel a title so it has an accessible name.

## Motion

- The panel fades and nudges in from its side over --duration-fast; reduced motion fades only.

## Responsive behavior

- Max width is 20rem or the viewport minus a margin; the runtime flips sides when there is no room.

## Targets

- **Hiccup (`:clj`)**: Works in server-rendered pages: load ui-runtime.js once.
- **Replicant (`:cljs`)**: Same markup; no state needed. Replicant keeps the popover attribute.
- **Squint (`:squint`)**: Same markup; ids must be stable across re-renders.

## Notes for AI

- The trigger :target and the content :id must match and be unique on the page.
- Keep the content to a few controls; a long form belongs in a dialog or a page.

## Related

- [Dialog](dialog.md): A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.
- [Context menu](context-menu.md): Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element.
- [Tooltip](tooltip.md): Short supporting text for an unfamiliar control, shown on hover and focus with pure CSS.
- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.

## Guidance for AI tools

Popover: A small anchored panel for contextual controls, built on the native Popover API so it works without app state. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
