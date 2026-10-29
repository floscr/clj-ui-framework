# Metric

> A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record.

- Type: Component (data, Numbers)
- Namespace: `ui.metric`
- Source: `src/ui/metric.cljc`
- Public fns: `metric-class-list`, `metric-classes`, `metric`, `metric-row`
- Keywords: metric, kpi, stat, statistic, number, counter, dashboard, stats row, delta, trend

## When to use

- The headline numbers of a dashboard, two to five of them in a metric-row above the detail.
- The stats of one record: days in a row, best streak, total spent.
- A number that needs context: what it is, its unit, how it changed.

## When not to use

- Use badge for a count inside a list row or next to a label.
- Use progress when the number is a share of a known total.
- Use table when the numbers belong to many records.

## Usage

```clojure
(require '[ui.metric :as metric])

(metric/metric-row {}
  (metric/metric {:label "Revenue" :value "48.2k" :unit "EUR" :delta "+12%" :trend :up :hint "vs last month"})
  (metric/metric {:label "Orders" :value "1,204" :delta "-3%" :trend :down :hint "vs last month"})
  (metric/metric {:label "Refunds" :value "18"}))
```

## Examples

### Record stats, flush inside a card

```clojure
(card/card {}
  (card/card-header {} [:h3 "Walk"] [:p "Mon to Fri"])
  (metric/metric-row {}
    (metric/metric {:flush true :label "Current streak" :value "0" :unit "days"})
    (metric/metric {:flush true :label "Best streak" :value "4" :unit "days"})))
```

## API reference

### `metric`

One labeled number: label above, the value at 28px with an optional unit beside it, then an optional delta and hint line.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string \| hiccup` | – | What the number is. |
| `:value` | `string \| hiccup` | – | The number, already formatted. |
| `:unit` | `string` | – | Unit or suffix beside the value, muted. |
| `:delta` | `string` | – | The change, for example +12%. |
| `:trend` | `:up \| :down \| :flat` | – | Colors the delta: success for up, danger for down, muted otherwise. Only set it when the direction has a meaning. |
| `:hint` | `string` | – | Short context beside the delta (vs last month, today). |
| `:flush` | `boolean` | `false` | No border or padding, for a metric inside a card or list. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `metric-row`

Equal-width columns at least 160px wide, 16px apart, that wrap on narrow screens. Tiles stretch to the same height.

Children: metric elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `metric-classes`

Class string for {:flush}.

### `metric-class-list`

Vector form of metric-classes.

## Accessibility

- The label and value are plain text in reading order; the delta color is backed by its sign and the hint.

## Motion

- None. Numbers use tabular figures so a changing value does not shift its neighbours.

## Responsive behavior

- A metric-row of four becomes two columns around 700px and one column on phones.

## Targets

- **Hiccup (`:clj`)**: Static markup.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Format the value before passing it (thousands separators, one decimal); the component does not format.
- A metric-row belongs directly under the page-header of a dashboard, before the chart or table.
- Do not color the value itself and do not make it larger than the h1; the 28px number is the emphasis.

## Related

- [Page](page.md): The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- [Badge](badge.md): A small pill for status, category or a count, with an optional leading icon.
- [Progress](progress.md): Shows how much of a known task is complete as a filled track.
- [Table](table.md): Records in columns from a headers vector and row vectors, with striped and bordered variants.

## Guidance for AI tools

Metric: A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
