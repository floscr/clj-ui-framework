# Settings rows

> Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen.

- Type: Component (layout, Page)
- Namespace: `ui.settings`
- Source: `src/ui/settings.cljc`
- Public fns: `settings-group`, `settings-row`
- Keywords: settings, preferences, options, row, switch row, label and control, form rows

## When to use

- A preferences or settings screen: each setting is one row with its name, an optional hint and its control.
- A list of toggles, selects or small actions that belong to one topic (notifications, appearance, account).
- Per-item controls in an admin list where every item gets the same choice (theme per service, access per member).

## When not to use

- Use form fields for a form that is submitted as a whole with validation; settings rows apply each change on its own.
- Use list-row for records with a title, description and metadata that lead somewhere.
- Use table when there are several columns to compare.

## Usage

```clojure
(require '[ui.settings :as settings])

(settings/settings-group {}
  (settings/settings-row {:label "Mentions" :hint "When someone mentions you in a comment."}
    (switch/switch-toggle {:checked true :on-change toggle-mentions!}))
  (settings/settings-row {:label "Weekly summary"}
    (switch/switch-toggle {:on-change toggle-summary!}))
  (settings/settings-row {:label "Time zone"}
    (select/select {:value "Europe/Vienna" :options zones})))
```

## Examples

### A choice per row with a segmented control

```clojure
(settings/settings-row {:label "Photos" :hint "Follows the system theme"}
  (button-group/button-group {:variant :boxed}
    (button-group/button-group-item {:active true} "Auto")
    (button-group/button-group-item {} "Light")
    (button-group/button-group-item {} "Dark")))
```

### A destructive action as a row

```clojure
(settings/settings-group {}
  (settings/settings-row {:label "Delete account" :hint "Removes your profile and projects. This cannot be undone."}
    (button/button {:variant :danger} "Delete account")))
```

## API reference

### `settings-group`

The bordered surface (bg-1, border-0, radius-surface) that holds the rows and divides them with border-subtle.

Children: settings-row elements.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `settings-row`

One setting: label and hint on the left, the control on the right, 64px minimum height. On narrow screens the control wraps under the text.

Children: The control: a switch, select, button group, button or short input. Several children sit 8px apart.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:label` | `string \| hiccup` | – | The setting's name, medium weight. |
| `:hint` | `string \| hiccup` | – | One line under the label explaining the effect. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

## Accessibility

- The label is visual: name the control itself (a switch's :label, an aria-label on a button group) so assistive tech reads it.
- Give the surrounding page-section a heading so the group has a name.

## Motion

- None.

## Responsive behavior

- Rows wrap: on a narrow screen a wide control (a select, a button group) drops under the label, right-aligned; a switch stays beside it.

## Targets

- **Hiccup (`:clj`)**: Static markup; controls carry HTMX attributes in their :attrs.
- **Replicant (`:cljs`)**: :class may be a vector.
- **Squint (`:squint`)**: Static markup.

## Notes for AI

- Put a settings-group inside a page-section with the topic as the section title. Do not add an uppercase label above the group and do not wrap each row in its own card.
- A settings row applies its change at once; it has no save button. For a form that saves as a whole use form fields plus an action-row.
- Keep hints to one line that states the effect, not the mechanism.

## Related

- [Page](page.md): The one page container with its title row, titled sections and action rows: column width, gutters and vertical rhythm set once for the whole app.
- [Switch](switch.md): A toggle for a setting that takes effect immediately.
- [Select](select.md): A compact choice field with a framework-drawn listbox, keyboard navigation and form support.
- [Button group](button-group.md): Related actions as one segmented control: ghost or boxed, with icon items, counts and separators.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.
- [Form](form.md): Labeled fields, inputs, textareas, checkboxes, radios, file and range inputs, joined groups and a search bar.

## Guidance for AI tools

Settings rows: Label-and-control rows on one bordered surface with hairline dividers: the shape of every preferences screen. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
