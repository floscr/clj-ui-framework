# Dialog

> A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps.

- Type: Component (disclosure, Overlays)
- Namespace: `ui.dialog`
- Source: `src/ui/dialog.cljc`
- Public fns: `dialog-class-list`, `dialog-classes`, `dialog`, `dialog-overlay`, `dialog-panel`, `dialog-header`, `dialog-body`, `dialog-footer`
- Keywords: modal, dialog, confirm, overlay, showModal

## When to use

- A decision that must interrupt: confirm a destructive action, pick before continuing.
- A short form (one to four fields) that returns to the page afterwards.

## When not to use

- Use popover for small anchored controls that should not dim the page.
- Use card or a route for long forms and record detail.
- Do not open a dialog for every delete; a danger button with a clear label is enough for reversible actions.

## Usage

```clojure
(require '[ui.dialog :as dialog])

;; Native <dialog>, opened with showModal()
(dialog/dialog {:id "confirm"}
  (dialog/dialog-header {} [:h3 "Delete project?"])
  (dialog/dialog-body {} "Its 12 files are removed for everyone.")
  (dialog/dialog-footer {}
    (button/button {:attrs {:onclick "confirm.close()"}} "Cancel")
    (button/button {:variant :danger} "Delete project")))
```

## Examples

### Overlay dialog driven by app state (Squint, Replicant)

```clojure
(when open?
  (dialog/dialog-overlay {:on-close #(close!)}
    (dialog/dialog-panel {}
      (dialog/dialog-header {} [:h3 "Rename"])
      (dialog/dialog-body {} (form/form-input {:value name :on-change set-name!}))
      (dialog/dialog-footer {}
        (button/button {:on-click close!} "Cancel")
        (button/button {:variant :primary :on-click save!} "Save changes")))))
```

## API reference

### `dialog`

Native <dialog>. Open it with element.showModal() for a backdrop and focus trapping; clicking the backdrop closes it.

Children: dialog-header, dialog-body, dialog-footer.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:open` | `boolean` | – | Render open (non-modal) on first paint. |
| `:id` | `string` | – | Id used to call showModal() / close(). |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-overlay`

Backdrop that covers the viewport and centers its children. For frameworks where a native <dialog> is impractical.

Children: A dialog-panel.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:on-close` | `fn` | – | Called when the backdrop is clicked. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-panel`

The surface inside an overlay: bg-1, radius-surface, floating shadow.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-header`

Title block.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-body`

Content block.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-footer`

Right-aligned actions.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `dialog-classes`

Class string for the dialog element.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Escape | Closes a native dialog (built in). Overlay dialogs close on backdrop click; wire Escape in the app. |
| Tab | Native dialogs trap focus inside the modal. |

## Accessibility

- Prefer the native <dialog>: role, focus trap and Escape come for free.
- Give the dialog a heading in dialog-header and reference it with aria-labelledby via :attrs.
- Overlay dialogs must return focus to the trigger when closed.

## Motion

- The panel fades and scales in over --duration-fast with --ease-enter; reduced motion keeps the fade only.

## Responsive behavior

- The panel is capped to the viewport with a margin; below 480px it fills the width.

## Targets

- **Hiccup (`:clj`)**: Use the native dialog with an inline onclick calling showModal().
- **Replicant (`:cljs`)**: Either variant; the overlay is driven by state and :on-close.
- **Squint (`:squint`)**: Use dialog-overlay plus dialog-panel; Eucalypt re-renders on state change.

## Notes for AI

- Name the object and the consequence in the title and body: Delete Harbour? Its 12 files are removed for everyone.
- Exactly one primary action in the footer; Cancel is secondary.

## Related

- [Popover](popover.md): A small anchored panel for contextual controls, built on the native Popover API so it works without app state.
- [Command palette](command.md): A searchable command menu in a native dialog with groups, icons, shortcut hints, a global hotkey and Alt quick-select.
- [Toast](toast.md): A brief floating confirmation for completed background work, shown by the JS runtime on every target.
- [Card](card.md): A bordered surface that groups related content and actions, plus a divided list variant.

## Guidance for AI tools

Dialog: A focused modal surface for decisions that need attention, as a native <dialog> or an overlay for reactive apps. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
