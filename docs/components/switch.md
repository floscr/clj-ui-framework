# Switch

> A toggle for a setting that takes effect immediately.

- Type: Component (inputs, Selection)
- Namespace: `ui.switch`
- Source: `src/ui/switch.cljc`
- Public fns: `switch-class-list`, `switch-classes`, `switch-toggle`
- Keywords: toggle, switch, on off, setting, checkbox alternative

## When to use

- A binary setting that applies as soon as it changes (notifications on, dark mode).
- Rows in a settings list where the control sits at the end of the row.

## When not to use

- Use form-checkbox inside a form that is submitted later, or for agree and include choices.
- Use button-group or tabs to switch between views of the same content.

## Usage

```clojure
(require '[ui.switch :as switch])

(switch/switch-toggle {:checked notifications?
                       :label "Email notifications"
                       :on-change #(toggle! :notifications)})
```

## Examples

### Disabled

```clojure
(switch/switch-toggle {:checked true :disabled true :label "Managed by your admin"})
```

## API reference

### `switch-toggle`

A <label> wrapping a hidden checkbox, a track and a thumb, plus the label text.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:checked` | `boolean` | – | On or off. |
| `:disabled` | `boolean` | – | Disables the input; the attribute is emitted only when true. |
| `:on-change` | `fn` | – | Change handler on :cljs and :squint. |
| `:label` | `string` | – | Visible label next to the track. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `switch-classes`

Class string for {:disabled}.

### `switch-class-list`

Vector form.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Space | Toggles the switch. |
| Tab | Moves focus to the hidden checkbox; the ring shows on the control. |

## Accessibility

- Built on a real checkbox input, so state is exposed natively.
- Always pass :label; a switch without a visible label needs an aria-label via :attrs.

## Motion

- Track color and thumb position animate over --duration-standard; reduced motion snaps.

## Responsive behavior

- The track is a fixed 40 by 22px target inside a taller label hit area.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled: the browser tracks the state. Submit it with a form or wire :attrs {:onchange "..."}.
- **Replicant (`:cljs`)**: Controlled: pass :checked from state and update it in :on-change.
- **Squint (`:squint`)**: Controlled; :disabled is omitted when falsy so Eucalypt does not disable the input.

## Notes for AI

- A switch applies immediately; if the page has a Save button, use form-checkbox instead.
- Put the label on the left and the switch on the right in settings rows.

## Related

- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Tabs](tabs.md): Switch between peer panels of one object, with no JS: hidden radios drive the panels.

## Guidance for AI tools

Switch: A toggle for a setting that takes effect immediately. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
