# Button

> A single action with quiet secondary states, press feedback and a loading spinner.

- Type: Component (actions, Buttons)
- Namespace: `ui.button`
- Source: `src/ui/button.cljc`
- Public fns: `button-class-list`, `button-classes`, `button`
- Keywords: action, submit, loading button, icon button, link button, press feedback

## When to use

- Any single action on a page, form or dialog: Save, Continue, Cancel.
- Short async work where a spinner on the button is enough feedback (:loading).
- Icon-only actions in toolbars and headers (:icon), optionally :round.

## When not to use

- Use button-group for two to five related options that act as one segmented control.
- Use toolbar for a cluster of icon buttons that should read as one surface.
- Use a plain link for navigation inside body text; :href on a button is for call-to-action styling only.

## Usage

```clojure
(require '[ui.button :as button])

(button/button {:variant :primary :icon-left :plus :on-click save!}
  "Add item")
```

## Examples

### Loading state

```clojure
(button/button {:variant :primary :loading saving?} "Save changes")
```

### Icon-only, round

```clojure
(button/button {:icon :x :round true :variant :ghost
                :attrs {:aria-label "Close"}})
```

### Link styled as a button

```clojure
(button/button {:href "/docs" :variant :secondary :icon-right :arrow-right}
  "Read the docs")
```

### Hiccup with an inline handler

```clojure
(button/button {:variant :danger :attrs {:onclick "deleteItem()"}} "Delete item")
```

## API reference

### `button`

Renders a <button>, or an <a> when :href is set.

Children: The label (text, icons, any hiccup).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:variant` | `:primary \| :secondary \| :ghost \| :danger \| :success \| :link` | `:secondary` | Visual weight. One :primary action per surface. |
| `:size` | `:sm \| :md \| :lg` | `:md` | Height, padding and icon size. |
| `:href` | `string` | – | Render as an anchor pointing at this URL. |
| `:on-click` | `fn` | – | Click handler on :cljs and :squint. Ignored on :clj (use :attrs). |
| `:disabled` | `boolean` | – | Disables the control. The attribute is emitted only when true. |
| `:loading` | `boolean` | – | Shows a spinner in place of the leading icon and disables the button. |
| `:icon-left` | `icon keyword` | – | Leading icon, e.g. :plus. |
| `:icon-right` | `icon keyword` | – | Trailing icon, e.g. :arrow-right. |
| `:icon` | `icon keyword` | – | Icon-only button (no children). Add an aria-label via :attrs. |
| `:round` | `boolean` | – | Fully round shape for icon-only buttons. |
| `:class` | `string` | – | Extra classes for layout. |
| `:attrs` | `map` | – | Extra HTML attributes merged onto the element (id, aria-*, data-*, onclick on :clj). |

### `button-classes`

Class string for {:variant :size :icon :round :loading}. Use it to style another element like a button, for example a popover-trigger or a label.

### `button-class-list`

Same as button-classes but returns a vector of class tokens (what Replicant needs).

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Enter / Space | Activates the button. |

## Accessibility

- Native <button> (or <a> with :href), so role and focus come for free.
- Icon-only buttons need an aria-label through :attrs.
- Loading sets disabled, so focus can move away; keep loading operations short.

## Motion

- Presses scale to 0.97 (0.94 for icon buttons) over --duration-fast.
- Hover changes color only. Reduced motion drops the press scale.

## Responsive behavior

- Size is fixed by :size; pick :lg for primary touch targets.
- Hover styles apply only on hover-capable pointers (gated by the CSS build).

## Targets

- **Hiccup (`:clj`)**: No event handlers. Use :attrs {:onclick "..."} or HTMX attributes.
- **Replicant (`:cljs`)**: :on-click becomes {:on {:click f}}; :class may be a vector of tokens.
- **Squint (`:squint`)**: :on-click is passed as a flat attribute; keywords are strings at runtime.

## Notes for AI

- The default variant is :secondary. Set :variant :primary on the one main action of a surface.
- Pass a changing label as children (Save, then Saved) instead of swapping two buttons.
- Never hand-write <button class="btn">; call the component so loading, icons and disabled stay consistent.
- Destructive actions use :variant :danger with a verb plus object label (Delete project).

## Related

- `button-group`: Button group — a cluster of related buttons rendered as a segmented
- `toolbar`: Toolbar — a rounded container that groups buttons (typically icon buttons)
- `spinner`
- `icon`

## Guidance for AI tools

Button: A single action with quiet secondary states, press feedback and a loading spinner. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
