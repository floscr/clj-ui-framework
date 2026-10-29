# Day grid

> One square per day in rows of seven under a weekday header, colored by what happened: the record of a habit or a countdown under its streak numbers.

- Type: Component (data, Numbers)
- Namespace: `ui.day-grid`
- Source: `src/ui/day_grid.cljc`
- Public fns: `day-grid-class-list`, `day-grid-classes`, `day-cell-class-list`, `day-cell-classes`, `day-grid`
- Keywords: day grid, habit, streak, heatmap, contribution graph, tracker, weeks, calendar strip

## When to use

- The last few weeks of a habit: which days were done, missed, excused or rest days.
- A countdown or challenge shown day by day, with the days still to come faded.
- Any per-day yes or no record that reads better as a pattern than as a list.

## When not to use

- Use calendar to pick a date, and calendar-events for days that hold events.
- Use progress when only the share of a total matters, not which days.
- Use metric for the streak numbers themselves; put the day grid under them.

## Usage

```clojure
(require '[ui.day-grid :as day-grid])

(day-grid/day-grid
  {:color :teal
   :days [{:date "Mon 26 Oct" :state :missed}
          {:date "Tue 27 Oct" :state :done}
          {:date "Wed 28 Oct" :state :missed}
          {:date "Thu 29 Oct" :state :done :today true}
          {:date "Fri 30 Oct" :state :future}
          {:date "Sat 31 Oct" :state :rest}
          {:date "Sun 1 Nov" :state :rest}]})
```

## Examples

### Under the streak numbers of a habit card

```clojure
(card/card {}
  (metric/metric-row {}
    (metric/metric {:flush true :label "Current streak" :value "1" :unit "day"})
    (metric/metric {:flush true :label "Best streak" :value "4" :unit "days"}))
  (day-grid/day-grid {:color :teal :days weeks}))
```

## API reference

### `day-grid`

Days in rows of seven, Monday first, under a header of weekday initials. Squares keep their aspect ratio and the grid is at most 320px wide. Done days fill with the color, missed days get a light orange tint, rest and excused days are outlined (excused darker), future days are faded, and today is ringed.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:days` | `vector of day maps` | – | In order, the first on the Monday column; pad the first week yourself. Each map: :date (tooltip fallback), :state (:done :missed :excused :rest :today :future, or nil for a plain empty day; other states get a day-grid-<state> class), :today (boolean, rings the day), :title (tooltip and accessible name). |
| `:color` | `:success \| :accent \| :teal \| :blue \| :orange \| :pink` | `:success` | Fill of done days and the today ring. |
| `:labels` | `vector of strings \| false` | `M T W T F S S` | Header labels, false for none. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `day-grid-classes`

Class string for {:color}.

### `day-grid-class-list`

Vector form of day-grid-classes.

### `day-cell-classes`

Class string for one day map ({:state :today}).

### `day-cell-class-list`

Vector form of day-cell-classes.

## Accessibility

- Each day with a :title or :date is an image with that text as its name and tooltip, so put the state in the title (Tue 27 Oct, done).
- The header initials are hidden from screen readers. The grid is a visual summary; keep the numbers that matter in text next to it (a metric).

## Motion

- None.

## Responsive behavior

- The grid fills its container up to 320px; squares shrink with it.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Build the day maps in the app: the component does no date math. Start the first row on a Monday and end the last on a Sunday.
- Use states for meaning, not color: :done, :missed, :excused, :rest, :future. Days before the habit started are :rest.
- Pick :color to match the ring or chart of the same goal; status colors keep their meaning.

## Related

- [Metric](metric.md): A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Calendar](calendar.md): A compact month grid date picker with navigation, today and selection, plus pure date math that runs on every target.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.

## Guidance for AI tools

Day grid: One square per day in rows of seven under a weekday header, colored by what happened: the record of a habit or a countdown under its streak numbers. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
