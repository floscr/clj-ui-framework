# Rating

> A star rating with half stars: pick by pointing at a star's half, clear by choosing the value again, or show a score read-only.

- Type: Component (inputs, Choices)
- Namespace: `ui.rating`
- Source: `src/ui/rating.cljc`
- Public fns: `value-str`, `steps`, `format-value`, `value-label`, `rating-class-list`, `rating-classes`, `rating`
- JS runtime: `ui.js.rating` (bundled in ui-runtime.js)
- Keywords: rating, stars, star rating, half stars, score, review

## When to use

- Your own score for something: a film, a book, a recipe.
- Showing a stored score on cards and lists, read-only and :sm.

## When not to use

- Use form-range when the value is continuous or has more than 20 steps.
- Use form-radio-group when the options are labelled choices, not a scale.
- Use metric for an average or a score from elsewhere that needs its number.

## Usage

```clojure
(require '[ui.rating :as rating])

(rating/rating {:value 3.5 :name "rating" :label "Your rating"})
(rating/rating {:value 4 :read-only true :size :sm})
```

## Examples

### Saving each pick with htmx

```clojure
[:form {:hx-post "/api/rating" :hx-trigger "change" :hx-swap "none"}
 [:input {:type "hidden" :name "id" :value film-id}]
 (rating/rating {:value 4 :name "rating" :label "Your rating"})]
```

### Whole stars, ten of them

```clojure
(rating/rating {:value 7 :max 10 :half false})
```

## API reference

### `rating`

Interactive: a radio group (a hidden 0 radio for none, one radio per step) under a row of stars. Read-only: an image of the value.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:value` | `number` | `0` | 0 or nil for none, else a step such as 3.5. |
| `:max` | `number` | `5` | Number of stars, up to 10. |
| `:half` | `boolean` | `true` | false allows whole stars only. |
| `:name` | `string` | – | Radio group name, submitted with a form. Generated when absent. |
| `:label` | `string` | `"Rating"` | Accessible name of the group. |
| `:size` | `:sm \| :md \| :lg` | `:md` | :sm for cards and lists. |
| `:read-only` | `boolean` | – | An image of the value with an aria-label, no inputs. |
| `:disabled` | `boolean` | – | Shown dimmed, not changeable. |
| `:on-change` | `fn [event]` | – | :cljs and :squint. The target is the checked radio; its value is "0" when cleared. |
| `:class` | `string` | – | On the wrapper. |
| `:attrs` | `map` | – | On the wrapper. |

### `steps`

The values a rating can take for {:max :half}, lowest first.

### `value-str`

A value as the radios carry it: "4", "3.5".

### `format-value`

A value for reading: 3.5 becomes "3½".

### `value-label`

A value for a screen reader: "3½ stars", "Not rated".

### `rating-classes`

Class string for {:size :read-only :disabled}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Arrow keys | Move between values (native radio group). |
| Space | Picks the focused value. |
| Backspace / Delete | Clears the rating (runtime). |

## Accessibility

- Interactive: role=radiogroup with the label, each radio named like "3½ stars", a 0 radio named "Not rated".
- Read-only: role=img with an aria-label such as "Rating: 4 out of 5".
- The focus ring sits around the stars while a radio has keyboard focus.

## Motion

- The fill moves over --duration-fast; a pick pops the stars one after another with --ease-spring.
- A hover preview dims the fill, only on devices that hover.
- Reduced motion drops the fill transition and the pop.

## Responsive behavior

- The width never changes with the value, so a rating can sit in a row of buttons.
- :md stars sit in a 36px tall row; :sm is for cards and lists.

## Targets

- **Hiccup (`:clj`)**: Uncontrolled: submit it in a form, or post each pick with htmx (hx-trigger change). Needs ui-runtime.js to clear by choosing the value again.
- **Replicant (`:cljs`)**: Pass :value and :on-change; read the value from the event target.
- **Squint (`:squint`)**: Same as Replicant; require ui.js.rating in the SPA for clearing and the pop.

## Notes for AI

- Read the value as a number from the checked radio: "0" means cleared.
- Use :read-only with :size :sm on cards; don't render an interactive rating per card.

## Related

- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.
- [Number field](number-field.md): A bounded numeric input with increment and decrement steppers and scroll-wheel support.
- [Metric](metric.md): A number with its label, unit, change and hint, as a bordered tile or a row of equal columns: the KPI row of a dashboard, the stats of a record.

## Guidance for AI tools

Rating: A star rating with half stars: pick by pointing at a star's half, clear by choosing the value again, or show a score read-only. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
