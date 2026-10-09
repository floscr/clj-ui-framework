# Tag input

> Turns short text into removable tags with a filtered suggestion dropdown; fully controlled by the host.

- Type: Component (inputs, Selection)
- Namespace: `ui.tag-input`
- Source: `src/ui/tag_input.cljc`
- Public fns: `tag-input-class-list`, `tag-input-classes`, `tag-pill-class-list`, `tag-pill-classes`, `tag-pill`, `tag-dropdown-item`, `tag-dropdown`, `tag-input`, `filter-tags`
- Keywords: tag input, multi-select, tokens, chips input, autocomplete

## When to use

- Several values from a known list (labels, people, categories) in a form.
- Free-text tags where each entry becomes a removable pill.

## When not to use

- Use select for exactly one value.
- Use chip for a small fixed set of filters shown all at once.

## Usage

```clojure
(require '[ui.tag-input :as tag-input])

(tag-input/tag-input {:tags selected
                      :input-value query
                      :open (seq suggestions)
                      :filtered-items suggestions
                      :active-index active
                      :placeholder "Add label…"
                      :on-input set-query!
                      :on-select add-tag!
                      :on-remove remove-tag!
                      :on-backspace remove-last!
                      :on-key-down handle-key!})
```

## Examples

### Filtering suggestions

```clojure
(tag-input/filter-tags all-tags selected query) ; excludes selected, matches case-insensitively
```

## API reference

### `tag-input`

The field: selected tag pills, a text input and the dropdown. All state lives in the host.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:tags` | `vector of {:label :value}` | – | Selected tags. |
| `:input-value` | `string` | – |  |
| `:open` | `boolean` | – | Show the dropdown. |
| `:filtered-items` | `vector` | – | Suggestions to show (already filtered). |
| `:active-index` | `int` | – | Highlighted suggestion. |
| `:placeholder` | `string` | `"Add tag..."` |  |
| `:disabled` | `boolean` | – |  |
| `:on-input` | `fn [value]` | – |  |
| `:on-remove` | `fn [tag]` | – |  |
| `:on-select` | `fn [item]` | – |  |
| `:on-backspace` | `fn []` | – | Backspace on an empty input. |
| `:on-clear` | `fn []` | – |  |
| `:on-focus` | `fn []` | – |  |
| `:on-blur` | `fn []` | – |  |
| `:on-key-down` | `fn [key-code]` | – | Arrow and Enter handling. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tag-pill`

One selected tag with a remove button.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – |  |
| `:on-remove` | `fn` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tag-dropdown`

The suggestion list.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:open` | `boolean` | – |  |
| `:items` | `vector` | – |  |
| `:on-select` | `fn` | – |  |
| `:active-index` | `int` | – |  |
| `:empty-text` | `string` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tag-dropdown-item`

One suggestion row.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string` | – |  |
| `:on-select` | `fn` | – |  |
| `:active` | `boolean` | – |  |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `tag-input-classes`

Class string for {:focused :disabled}.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Type | Filters suggestions. |
| ArrowUp / ArrowDown | Move the highlight (host handles :on-key-down). |
| Enter | Adds the highlighted suggestion. |
| Backspace on empty | Removes the last tag. |

## Accessibility

- Pills have remove buttons with labels; the dropdown items are buttons. Give the field a label with form-field.

## Motion

- Focus ring and dropdown fade over --duration-fast.

## Responsive behavior

- Pills wrap inside the field; the dropdown is capped and scrolls.

## Targets

- **Hiccup (`:clj`)**: Renders the current state statically; no interaction without app JS.
- **Replicant (`:cljs`)**: Controlled; keep tags, query, open and active index in state.
- **Squint (`:squint`)**: Controlled; filter-tags is available on all targets.

## Notes for AI

- The component owns no state; wire every callback or use a small reducer in the host.

## Related

- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Chip](chip.md): A small selectable pill for filters, tags and presets, with an optional color dot.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Tag input: Turns short text into removable tags with a filtered suggestion dropdown; fully controlled by the host. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
