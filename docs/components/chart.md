# Chart

> Charts for a dashboard without a charting library: a time series as a line over a fading area with a comparison line and a crosshair, ranked rows with a rail under each label, a donut for shares and a sparkline for the trend beside a number.

- Type: Component (data, Numbers)
- Namespace: `ui.chart`
- Source: `src/ui/chart.cljc`
- Public fns: `round1`, `pct`, `nice-max`, `format-number`, `plot-points`, `line-path`, `area-path`, `donut-arcs`, `chart-class-list`, `chart-classes`, `legend`, `chart`, `area-chart`, `bar-list`, `donut`, `sparkline`
- Keywords: chart, graph, area chart, line chart, time series, crosshair, bar list, ranking, donut, pie, sparkline, trend, analytics, dashboard, legend

## When to use

- One measure over days, weeks or hours, optionally against the period before: area-chart.
- The top entries of a list by one number (pages, sources, countries): bar-list.
- How a total splits into two to six parts: donut.
- The recent course of a number, beside it in a metric: sparkline.

## When not to use

- Use metric alone when the number matters and its course does not.
- Use progress for one value against a known total.
- Use day-grid for a yes or no record per day.
- Use table when rows carry several numbers; a bar-list shows one.
- For more than about 120 points, or several series with their own scales, bucket the data first (weeks instead of days) or use a charting library.

## Usage

```clojure
(require '[ui.chart :as chart]
         '[ui.card :as card])

(card/card {}
  (chart/chart {:title "Visitors"
                :description "112 in the last 30 days, +19% on the 30 before"}
    (chart/area-chart
      {:label "This period"
       :compare-label "Period before"
       :points [{:label "Mon 16 Nov" :value 3 :compare 1}
                {:label "Tue 17 Nov" :value 1 :compare 4}
                {:label "Wed 18 Nov" :value 5 :compare 2}]})))
```

## Examples

### Ranked rows that link to their detail

```clojure
(chart/bar-list
  {:rows [{:label "Blog" :description "/blog" :value 48 :note "12%" :href "/page?p=/blog"}
          {:label "Notes" :description "/notes" :value 27 :note "7%" :href "/page?p=/notes"}]})
```

### Shares of a whole

```clojure
(chart/donut
  {:center-label "visitors"
   :segments [{:label "Direct" :value 250}
              {:label "Search" :value 27}
              {:label "Other sites" :value 32}]})
```

### A sparkline in a metric

```clojure
(metric/metric {:label "Visitors" :value "112" :delta "+19%" :trend :up
                :chart (chart/sparkline {:values [9 13 14 17 44 25 31 23 17 22 39 21 12]
                                         :label "Visitors per week, last 13 weeks"})})
```

## API reference

### `chart`

The frame of a chart: an h3 title with an optional description on the left, actions on the right, then the children. It has no surface; put it in a card.

Children: The chart and what belongs to it (a legend, a note).

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:title` | `string \| hiccup` | – | What is charted. |
| `:description` | `string \| hiccup` | – | One line under the title: the total, the range, the change. |
| `:actions` | `hiccup` | – | Controls at the right of the title row, usually a boxed button-group as the range picker. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `area-chart`

A time series as a line over an area that fades to the baseline. With :compare values a dashed line of the comparison series is drawn behind it and a legend under it. Each point has a column over the plot: hovering or tapping it shows a crosshair, a dot per series and a card with the values. The plot is 200px high (--chart-h) and as wide as its container.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:points` | `vector of point maps` | – | Oldest first. Each map: :label (name in the value card, "Thu 5 Nov"), :value (number), :compare (optional number of the comparison series), :axis-label (optional, labels this point on the x axis; without any, about five evenly spaced points are labeled with :label). |
| `:label` | `string` | `"Value"` | Name of the series in the card and the legend. |
| `:compare-label` | `string` | `"Before"` | Name of the comparison series. |
| `:color` | `:accent \| :blue \| :teal \| :orange \| :pink \| :success` | `the first series token` | Color of the line, the area and the dots. |
| `:max` | `number` | `nice-max of the data` | Top of the value axis. Set it to give several charts the same scale. |
| `:curve` | `:smooth \| :linear` | `:smooth` | Bent through the points without leaving the plot, or straight segments. |
| `:format` | `fn number -> string` | `format-number` | Text of a value on the axis and in the card. |
| `:legend` | `boolean` | `true with a comparison series` | false hides the legend. |
| `:loading` | `boolean` | `false` | A skeleton of the plot's size instead of the chart. |
| `:empty-label` | `string` | `"No data for this range"` | Shown in an empty-state when :points is empty. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `bar-list`

Ranked rows: the label with its value at the end of the line and a 4px rail under them, filled to the row's share of the largest value. A row with :href is a link with a hover surface.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:rows` | `vector of row maps` | – | Largest first. Each map: :label, :description (muted, after the label), :value (number, sets the rail), :display (the value as shown), :note (muted, after the value, such as the share), :href, :active. |
| `:max` | `number` | `the largest :value` | Value of a full rail. |
| `:color` | `:accent \| :blue \| :teal \| :orange \| :pink \| :success` | `the first series token` | Color of the rails. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `donut`

Shares of a whole as a 144px ring with a number in the middle and a legend beside it (under it when there is no room): name, value and share per segment. Segments are colored in order: the four series tokens, then two neutrals.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:segments` | `vector of segment maps` | – | Largest first, at most six. Each map: :label, :value (number), :display (the value as shown), :active (thicker segment, bolder legend row). |
| `:center-value` | `string \| hiccup` | `the total` | The number in the middle. |
| `:center-label` | `string` | – | A line under it, such as the unit. |
| `:format` | `fn number -> string` | `format-number` | Text of a value. |
| `:legend` | `boolean` | `true` | false leaves the legend out. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `sparkline`

A 104 by 36px trend line over a light tint, without axes. Scaled from 0 to its own largest value.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:values` | `vector of numbers` | – | Oldest first. |
| `:label` | `string` | – | Accessible name. Without one the sparkline is hidden from screen readers, which is right when the number beside it says enough. |
| `:color` | `:accent \| :blue \| :teal \| :orange \| :pink \| :success` | `the first series token` |  |
| `:area` | `boolean` | `true` | false draws the line alone. |
| `:zero` | `boolean` | `true` | false scales from the smallest value instead of 0, for values that move far from zero (a rate between 40 and 50). Use it with :area false: a tint down to a baseline that is not zero would mislead. |
| `:curve` | `:smooth \| :linear` | `:smooth` |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `legend`

A wrapping row of series names, each after a dot in its color. area-chart adds its own; use this one for charts you compose.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:items` | `vector of {:label :color :compare}` | – | :color is one of the chart colors, :compare true gives the muted dot of a comparison series. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `nice-max`

The axis top for a largest value: rounded up to a round number whose half is round too, at least 4.

### `format-number`

A number as text: whole numbers without a decimal, others with one.

### `plot-points`

Values as [x y] pairs in a {:w :h :top :low :pad :edges} box.

### `line-path`

The SVG path through [x y] pairs, {:curve :smooth | :linear, :h box height}.

### `area-path`

A line path closed down to the bottom of the box.

### `donut-arcs`

Segments with :dash and :offset for a circle of radius 48.

### `pct`

A share of a total in percent with one decimal.

### `round1`

A number rounded to one decimal.

### `chart-classes`

Class string for a base class and {:color}.

### `chart-class-list`

Vector form of chart-classes.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves through the linked rows of a bar-list. The points of an area-chart are not tab stops. |

## Accessibility

- area-chart carries a visually hidden table of every point with both series, so screen readers get the numbers; the plot, axes and value cards are for sighted reading.
- The points are focusable by pointer and touch only (tabindex -1): thirty tab stops in a chart would be worse than the table.
- donut names every segment with its share in the ring's label, and the legend repeats them as text.
- Give a sparkline a :label when the trend is not said in text next to it.
- The series color is never the only signal: the comparison line is dashed and the legend names both.

## Motion

- The line and area draw in from the left on first render, rails grow from the left, both over the considered duration.
- The crosshair, dots and value card fade in over the fast duration; a hovered donut segment thickens.
- All of it is skipped under reduced motion (the base styles collapse durations).

## Responsive behavior

- Charts fill their container. The area chart keeps its 200px height; set --chart-h on it for another.
- On touch devices a tap on a point shows its crosshair and card, a tap elsewhere hides it.
- The donut's legend wraps under the ring when the container is narrower than about 330px.
- Axis labels do not thin out by width: pass fewer :axis-label points for a chart that is always narrow.

## Targets

- **Hiccup (`:clj`)**: Static markup; the crosshair needs no script, so it works on server-rendered pages without the runtime.
- **Replicant (`:cljs`)**: :class may be a vector. Inline styles are maps of custom properties.
- **Squint (`:squint`)**: Static markup. Inline styles are string-keyed maps of custom properties.

## Notes for AI

- Build the point, row and segment maps in the app; the components do no date math and no sorting.
- Put a chart in a card: (card/card {} (chart/chart {:title ...} (chart/area-chart ...))).
- The range picker is a boxed button-group in the chart's :actions (or the page-header's), not tabs.
- Give charts that are compared with each other the same :max.
- Spend color with intent: one series color per chart, the comparison stays muted. Do not use status colors for series unless the series is a status.
- Use :display and :format for units and separators; :value stays a number.

## Related

- [Metric](metric.md): A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Day grid](day-grid.md): One square per day in rows of seven under a weekday header, colored by what happened: the record of a habit or a countdown under its streak numbers.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.
- [Empty state](empty-state.md): A centered placeholder with an optional icon for lists and views that have nothing to show yet.

## Guidance for AI tools

Chart: Charts for a dashboard without a charting library: a time series as a line over a fading area with a comparison line and a crosshair, ranked rows with a rail under each label, a donut for shares and a sparkline for the trend beside a number. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
