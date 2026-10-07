# Calendar

A month-grid date picker and event calendar inspired by [shadcn/radix Calendar](https://ui.shadcn.com/docs/components/radix/calendar) and the org-mode-agenda-cli.

Two namespaces: `ui.calendar` for the core date picker, `ui.calendar-events` for event grid, ticker strip, and agenda list.

## Date Picker

A compact month grid with navigation, today highlighting, and date selection.

```clojure
;; Minimal static calendar
(calendar/calendar {:year 2026 :month 3 :today-str "2026-03-29"})

;; Interactive with selection
(calendar/calendar {:year 2026 :month 3
                    :today-str "2026-03-29"
                    :selected-date "2026-03-15"
                    :on-select (fn [date-str] (println date-str))
                    :on-prev-month (fn [_] ...)
                    :on-next-month (fn [_] ...)})
```

### Props

| Prop             | Type   | Description                               |
|------------------|--------|-------------------------------------------|
| `:year`          | int    | Displayed year (e.g. 2026)                |
| `:month`         | int    | Displayed month (1–12)                    |
| `:today-str`     | string | Today's date as `"YYYY-MM-DD"`            |
| `:selected-date` | string | Selected date as `"YYYY-MM-DD"` or nil    |
| `:on-select`     | fn     | `(fn [date-str] ...)` — on day click      |
| `:on-prev-month` | fn     | Called on prev-month button click          |
| `:on-next-month` | fn     | Called on next-month button click          |
| `:class`         | string | Additional CSS classes                    |
| `:attrs`         | map    | Additional HTML attributes                |

## Event Grid

A full month grid with colored event pills inside day cells.

```clojure
(cal-events/calendar-event-grid
  {:year 2026 :month 3
   :today-str "2026-03-29"
   :events [{:title "Meeting" :date "2026-03-29" :time-start "10:00" :color :accent}]
   :on-select (fn [date-str] ...)
   :on-prev-month (fn [_] ...)
   :on-next-month (fn [_] ...)
   :on-event-click (fn [event-map] ...)
   :max-visible 3})
```

## Day Ticker

Horizontal scrollable strip showing days with event dot indicators.

```clojure
(cal-events/ticker-strip
  {:days [{:date "2026-03-29" :day-num 29 :day-label "Su"} ...]
   :today-str "2026-03-29"
   :selected "2026-03-29"
   :events events
   :on-select (fn [date-str] ...)})
```

## Agenda List

Vertical list of events grouped by day. Days without events are dropped —
except today when `:today-str` is passed: today's group is always shown,
highlighted (`.cal-agenda-day-today`, accent edge + label), and renders an
`agenda-day-empty` card ("Nothing scheduled / A clear day") when it has no
events. Pass `:on-add-event` to give that card an **Add** button.

```clojure
(cal-events/agenda-list
  {:days [{:date "2026-03-29" :label "Today"} ...]
   :events events
   :today-str "2026-03-29"                       ;; highlight + always show today
   :today-empty-title "Nothing scheduled"        ;; optional, this is the default
   :today-empty-hint "A clear day"               ;; optional, this is the default
   :on-add-event (fn [date-str] ...)             ;; optional Add button on the empty card
   :on-event-click (fn [event-map] ...)})
```

## Event Data Format

Events are plain maps:

```clojure
{:title      "Team standup"
 :date       "2026-03-29"     ;; YYYY-MM-DD
 :time-start "09:00"          ;; HH:MM or nil
 :time-end   "09:30"          ;; HH:MM or nil
 :color      :accent          ;; see Event Colors below, or nil
 :done?      false}
```

## Event Colors

Eight named colors, all with automatic dark-mode variants. Pass `nil` for the default gray.

- Semantic tokens: `:accent`, `:danger`, `:success`, `:warning`
- Categorical palettes (for coloring sources/series that need to stay distinguishable): `:blue`, `:teal`, `:pink`, `:orange`

The full set is `ui.calendar-events/event-colors`. Hues are spaced so all eight read as distinct: danger 25 · orange 55 · warning 76 · success 152 · teal 195 · blue 245 · accent 286 · pink 345.

## Date Utilities

All date math is pure (no JS Date dependency) and works on all targets:

- `(days-in-month year month)` — days in a month (handles leap years)
- `(day-of-week year month day)` — 0=Mon..6=Sun
- `(first-day-of-week year month)` — weekday of the 1st
- `(calendar-days year month)` — full grid including prev/next month padding
- `(prev-month year month)` / `(next-month year month)` — returns `[year month]`
- `(date-str year month day)` — formats as `"YYYY-MM-DD"`

## CSS Classes

The calendar uses `cal-` prefixed classes. Key states on day cells:

- `.cal-day-today` — today's date
- `.cal-day-selected` — currently selected date
- `.cal-day-outside` — days from adjacent months
- `.cal-day-disabled` — unselectable days

Event pills use color classes: `.cal-event-accent`, `.cal-event-danger`, `.cal-event-success`, `.cal-event-warning`, `.cal-event-blue`, `.cal-event-teal`, `.cal-event-pink`, `.cal-event-orange`, `.cal-event-default`.
