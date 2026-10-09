# Calendar

> A compact month grid date picker with navigation, today and selection, plus pure date math that runs on every target.

- Type: Component (inputs, Dates)
- Namespace: `ui.calendar`
- Source: `src/ui/calendar.cljc`
- Public fns: `leap-year?`, `days-in-month`, `day-of-week`, `day-of-year`, `iso-week`, `first-day-of-week`, `pad2`, `date-str`, `month-name`, `short-month-name`, `prev-month`, `next-month`, `calendar-days`, `inc-date`, `dec-date`, `week-start`, `week-days`, `calendar-class-list`, `calendar-classes`, `day-cell-class-list`, `day-cell-classes`, `calendar-header`, `calendar-weekdays`, `calendar-day`, `calendar`
- Keywords: calendar, date picker, month grid, date math, iso week

## When to use

- Picking a single date inline (a filter, a form field with the grid visible).
- Any date arithmetic in shared code: the helpers are pure and need no JS Date.

## When not to use

- Use calendar-events for grids with events, agendas and week or day timelines.
- Use form-input :type :date when a native picker is acceptable and space is tight.

## Usage

```clojure
(require '[ui.calendar :as calendar])

(calendar/calendar {:year 2026 :month 10 :today-str "2026-10-09"
                    :selected-date selected
                    :on-select set-date!
                    :on-prev-month prev! :on-next-month next!})
```

## Examples

### Date helpers

```clojure
(calendar/days-in-month 2026 2)        ; 28
(calendar/day-of-week 2026 10 9)       ; 4 (0 = Monday)
(calendar/iso-week 2026 10 9)          ; 41
(calendar/calendar-days 2026 10)       ; 42 day maps incl. padding
```

## API reference

### `calendar`

The full picker: header with month navigation, weekday row and the day grid.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:year` | `int` | – |  |
| `:month` | `int` | – | 1 to 12. |
| `:today-str` | `"YYYY-MM-DD"` | – | Today, for highlighting. |
| `:selected-date` | `"YYYY-MM-DD" \| nil` | – |  |
| `:on-select` | `fn [date-str]` | – |  |
| `:on-prev-month` | `fn` | – |  |
| `:on-next-month` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `calendar-header`

Month and year title with previous and next buttons.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:year` | `int` | – |  |
| `:month` | `int` | – |  |
| `:on-prev-month` | `fn` | – |  |
| `:on-next-month` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `calendar-weekdays`

Weekday header row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:labels` | `vector of 7 strings` | – | Mon to Sun. |
| `:today-index` | `int \| nil` | – | Column to mark as today. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `calendar-day`

One day cell.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:day` | `day map` | – | From calendar-days. |
| `:today-str` | `string` | – |  |
| `:selected-date` | `string` | – |  |
| `:on-select` | `fn [date-str]` | – |  |
| `:disabled?` | `boolean` | – |  |

### `calendar-days`

The 6 by 7 grid of day maps for a month, padded with adjacent months.

### `days-in-month`

Days in a month, leap years included.

### `day-of-week`

0 = Monday to 6 = Sunday.

### `iso-week`

ISO 8601 week number.

### `date-str`

Formats year, month, day as YYYY-MM-DD.

### `week-days`

The seven day maps of the week containing a date.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through navigation and day buttons. |
| Enter / Space | Selects the focused day. |

## Accessibility

- Days are buttons with the full date as their label; today and selected states are classes plus text.

## Motion

- Day hover tint over --duration-fast.

## Responsive behavior

- Fits its content; cells are 36px targets, use hit-area on small screens.

## Targets

- **Hiccup (`:clj`)**: Static grid; wire day clicks with :attrs or links in your own cell renderer.
- **Replicant (`:cljs`)**: Controlled through :selected-date and the callbacks.
- **Squint (`:squint`)**: Same as Replicant.

## Notes for AI

- All dates are YYYY-MM-DD strings; the component never touches JS Date.
- See src/ui/calendar.md for the full documentation of both calendar namespaces.

## Related

- [Calendar events](calendar-events.md): Event-aware calendar views: month grids with pills, a day ticker, an agenda list and day and week timelines, all from plain event maps.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Calendar: A compact month grid date picker with navigation, today and selection, plus pure date math that runs on every target. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
