# Calendar events

> Event-aware calendar views: month grids with pills, a day ticker, an agenda list and day and week timelines, all from plain event maps.

- Type: Component (data, Dates)
- Namespace: `ui.calendar-events`
- Source: `src/ui/calendar_events.cljc`
- Public fns: `event-color-class`, `events-for-date`, `cell-events`, `format-time`, `event-time-display`, `parse-int*`, `round*`, `hhmm->minutes`, `event-duration-minutes`, `format-duration`, `format-hour-label`, `event-pill-class-list`, `event-pill-classes`, `ticker-day-class-list`, `ticker-day-classes`, `agenda-event-class-list`, `agenda-event-classes`, `task-check`, `event-pill`, `event-day-cell`, `calendar-event-grid`, `month-grid`, `ticker-dot`, `ticker-day-item`, `ticker-strip`, `agenda-event-row`, `agenda-day-group-class-list`, `agenda-day-empty`, `agenda-day-group`, `agenda-list`, `day-timeline`, `week-timeline`
- Keywords: calendar, events, agenda, week view, day view, month view, event pills

## When to use

- A month overview with events per day (full desktop or compact phone variant).
- An agenda grouped by day, a horizontal day strip, or an hour-based day and week timeline.

## When not to use

- Use calendar for a plain date picker without events.
- Use table for records that merely have a date column.

## Usage

```clojure
(require '[ui.calendar-events :as cal-events])

(cal-events/month-grid
  {:year 2026 :month 10 :today-str "2026-10-09" :variant :full
   :events [{:title "Standup" :date "2026-10-09" :time-start "09:00" :time-end "09:30" :color :accent}]
   :on-select select-day! :on-event-click open-event!})
```

## Examples

### Agenda with today always shown

```clojure
(cal-events/agenda-list {:days days :events events :today-str today
                         :on-add-event add! :on-event-click open!})
```

### Week timeline

```clojure
(cal-events/week-timeline {:days week :events events :today-str today :now-minutes 615})
```

## API reference

### `month-grid`

Frameless month view (the host brings the toolbar). :full is the roomy desktop month, :compact the phone month with dots.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:year` | `int` | – |  |
| `:month` | `int` | – |  |
| `:events` | `vector of event maps` | – |  |
| `:today-str` | `string` | – |  |
| `:selected-date` | `string` | – |  |
| `:variant` | `:full \| :compact` | `:full` |  |
| `:week-numbers?` | `boolean` | – | :full only. |
| `:max-visible` | `int \| fn [date-str day-events]` | – | Pills before a +N more button (:full); a fn sizes it per cell, e.g. to what fits. |
| `:highlight-week` | `date string` | – | That date's row gets the active week band (:compact). |
| `:on-select` | `fn [date-str]` | – |  |
| `:on-double-click` | `fn [date-str]` | – |  |
| `:on-more-click` | `fn [date-str]` | – |  |
| `:on-event-click` | `fn [event]` | – |  |
| `:on-event-context-menu` | `fn [event e]` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `calendar-event-grid`

Framed month grid with navigation header and event pills.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:year` | `int` | – |  |
| `:month` | `int` | – |  |
| `:today-str` | `string` | – |  |
| `:selected-date` | `string` | – |  |
| `:events` | `vector` | – |  |
| `:on-select` | `fn` | – |  |
| `:on-prev-month` | `fn` | – |  |
| `:on-next-month` | `fn` | – |  |
| `:on-event-click` | `fn` | – |  |
| `:on-event-context-menu` | `fn` | – |  |
| `:max-visible` | `int` | – |  |
| `:pill-layout` | `:inline \| :stacked` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `event-pill`

One event inside a day cell; task events render as a checkbox row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:event` | `event map` | – |  |
| `:on-click` | `fn` | – |  |
| `:on-context-menu` | `fn` | – |  |
| `:layout` | `:inline \| :stacked` | – |  |

### `ticker-strip`

Horizontal scrollable day strip with event dots.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:days` | `vector of {:date :day-num :day-label}` | – |  |
| `:today-str` | `string` | – |  |
| `:selected` | `string` | – |  |
| `:events` | `vector` | – |  |
| `:on-select` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `agenda-list`

Events grouped by day; today is always shown, with an empty card when it has nothing.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:days` | `vector of {:date :label}` | – |  |
| `:events` | `vector` | – |  |
| `:today-str` | `string` | – |  |
| `:today-empty-title` | `string` | – |  |
| `:today-empty-hint` | `string` | – |  |
| `:on-add-event` | `fn [date-str]` | – |  |
| `:on-event-click` | `fn` | – |  |
| `:on-event-context-menu` | `fn` | – |  |
| `:on-toggle-done` | `fn [event]` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `day-timeline`

One day on an hour axis with positioned event blocks and a now line.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:events` | `vector` | – |  |
| `:date` | `string` | – |  |
| `:today-str` | `string` | – |  |
| `:now-minutes` | `int` | – |  |
| `:on-event-click` | `fn` | – |  |
| `:on-event-context-menu` | `fn` | – |  |
| `:hour-height` | `px` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `week-timeline`

Seven day columns on a shared hour axis.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:days` | `vector of day maps` | – |  |
| `:events` | `vector` | – |  |
| `:today-str` | `string` | – |  |
| `:now-minutes` | `int` | – |  |
| `:on-event-click` | `fn` | – |  |
| `:on-event-context-menu` | `fn` | – |  |
| `:hour-height` | `px` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `events-for-date`

Events whose :date matches.

### `cell-events`

A date's events in day-cell order: by time, done ones last (first into +N more).

### `event-duration-minutes`

Minutes between :time-start and :time-end.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Day cells and event pills are buttons. |
| Enter | Opens the focused event or day. |

## Accessibility

- Event colors are categorical; titles and times are always text. Task pills are real checkboxes.

## Motion

- Pill and cell hover tints over --duration-fast.

## Responsive behavior

- Use :compact on phones and the ticker plus agenda for the narrowest layouts; the week timeline scrolls horizontally.

## Targets

- **Hiccup (`:clj`)**: Renders statically from the event vector; actions via links in :attrs or page reloads.
- **Replicant (`:cljs`)**: All callbacks available; events are plain maps.
- **Squint (`:squint`)**: Same as Replicant; keep event keys as strings or keywords consistently.

## Notes for AI

- Event maps: {:title :date :time-start :time-end :color :done? :task?}; colors are :accent :danger :success :warning :blue :teal :pink :orange.
- Full documentation with markup contracts lives in src/ui/calendar.md.

## Related

- [Calendar](calendar.md): A compact month grid date picker with navigation, today and selection, plus pure date math that runs on every target.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [Context menu](context-menu.md): Secondary actions close to the object: a right-click (or long-press on touch) menu for any wrapped element.

## Guidance for AI tools

Calendar events: Event-aware calendar views: month grids with pills, a day ticker, an agenda list and day and week timelines, all from plain event maps. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
