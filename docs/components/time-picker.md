# Time picker

> A time of day, or a start and an end with the length between them, typed into a field or picked from a quarter-hour list.

- Type: Component (inputs, Special inputs)
- Namespace: `ui.time-picker`
- Source: `src/ui/time_picker.cljc`
- Public fns: `parse-time`, `format-time`, `format-length`, `parse-value`, `value-str`, `start-slots`, `end-slots`, `move-start`, `time-picker-class-list`, `time-picker-classes`, `time-picker`
- JS runtime: `ui.js.time-picker` (bundled in ui-runtime.js)
- Keywords: time picker, time input, time range, start end, duration, schedule, hour minute

## When to use

- Setting when something happens: the time of an event, a reminder, a scheduled task.
- A start and an end where the length matters, like a meeting or a time block.
- Forms used with a keyboard and on a phone alike: typing 1430 is as quick as picking a slot.

## When not to use

- Use calendar for the date; put a time picker next to it for the time.
- Use chip for a handful of preset times without a custom one.
- Use number-field for a length on its own, like 45 minutes.

## Usage

```clojure
(require '[ui.time-picker :as time-picker])

(time-picker/time-picker {:start "14:30" :end "15:45"
                          :on-change #(set-time! (time-picker/parse-value (.. % -target -value)))})
```

## Examples

### One time

```clojure
(time-picker/time-picker {:value "09:00" :name "remind-at"})
```

### Start and end

```clojure
(time-picker/time-picker {:start "14:30" :end "15:45"})
```

### Empty, waiting for a start

```clojure
(time-picker/time-picker {:range true :placeholder "All day"})
```

## API reference

### `time-picker`

A field with a start segment, and in range mode an end segment and the length. The time-picker runtime opens the slot lists, reads typed times and keeps a hidden input in sync.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `string` | – | `HH:MM`, `HH:MM-HH:MM` or empty. |
| `:start` | `string` | – | The start as `HH:MM`; overrides :value. |
| `:end` | `string` | – | The end as `HH:MM`; overrides :value and turns on :range. Dropped when it isn't after the start. |
| `:range` | `boolean` | `false` | Shows the end segment and the length once there is a start. |
| `:step` | `minutes` | `15` | Minutes between slots and per arrow key. |
| `:placeholder` | `string` | `"Add time"` | Text of the empty field. |
| `:clearable` | `boolean` | `true` | A clear button once there is a time. |
| `:disabled` | `boolean` | – |  |
| `:name` | `string` | – | On the hidden value input (form submission). |
| `:id` | `string` | – | On the hidden value input. |
| `:on-change` | `fn [event]` | – | :cljs and :squint; the value is event.target.value, split it with parse-value. |
| `:class` | `string` | – | On the field. |
| `:attrs` | `map` | – | On the field. |

### `parse-value`

{:start :end} in minutes from `HH:MM` or `HH:MM-HH:MM`.

### `value-str`

The value string for start and end minutes.

### `parse-time`

Minutes from typed text: 14:30, 1430, 9, 2:30pm, 2p.

### `format-time`

`HH:MM` for minutes from midnight.

### `format-length`

45m, 1h, 1h 15m.

### `start-slots`

Every step of the day.

### `end-slots`

From one step after a start up to 12 hours later, before midnight.

### `move-start`

A new start that keeps the length of the range.

### `time-picker-classes`

Class string for {:range :empty :disabled}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Typing | Shows the slot it means; text that isn't a time turns red. |
| ArrowUp / ArrowDown | Steps to the previous or next slot. |
| Enter | Commits the typed or arrowed time; picking a start moves on to the end. |
| Escape | Closes the list and puts the committed time back. |
| Tab | Commits what is typed and moves on. |

## Accessibility

- Each segment is a labelled combobox (Start time, End time) with aria-expanded; the list is a listbox of options with aria-selected.
- Focus stays in the segment while the list is open, so typing and the arrow keys work throughout.

## Motion

- The list rises in over --duration-standard with --ease-enter; none with reduced motion.
- Slot hover and the active segment tint over --duration-fast.

## Responsive behavior

- Fills its container; the list is 11rem wide, fixed under the active segment and flipped above it near the bottom of the screen.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled: submit the hidden input with a form. Needs ui-runtime.js.
- **Replicant (`:cljs`)**: Pass :start/:end or :value and :on-change; the runtime commits and fires change on the hidden input.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.time-picker in the SPA.

## Notes for AI

- Store the value as `HH:MM-HH:MM`: it is org's time range and what parse-value reads.
- Put preset times as chips under an empty picker rather than replacing it.

## Related

- [Calendar](calendar.md): A compact month grid date picker with navigation, today and selection, plus pure date math that runs on every target.
- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.
- [Number field](number-field.md): A bounded numeric input with increment and decrement steppers and scroll-wheel support.
- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Time picker: A time of day, or a start and an end with the length between them, typed into a field or picked from a quarter-hour list. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
