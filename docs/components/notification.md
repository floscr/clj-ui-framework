# Notification

> Cards for things that happen out of view, like a copy or a command running in the background, stacked with the newest in front and the rest tucked behind until you reach for them.

- Type: Component (feedback, Messages)
- Namespace: `ui.notification`
- Source: `src/ui/notification.cljc`
- Public fns: `notification-class-list`, `notification-classes`, `notification`, `notification-stack-class-list`, `notification-stack-classes`, `notification-stack`
- Keywords: notification, toast, toast stack, sonner, background job, copied, snackbar, live activity

## When to use

- Work that runs in the background and finishes later: a shell command, an export, a sync. The card starts as :loading and turns into its result in place.
- A short confirmation that needs more than one word, or an action like Open or Undo.
- Several of these at once: the stack keeps them to one card's height until you hover or tap it.

## When not to use

- Use toast for a one-line flash the JS runtime shows and removes on its own, or from an HTMX response.
- Use alert for a message that belongs to the page and must stay until it is resolved.
- Use processing-bar for a list of uploads or jobs you watch as they progress.

## Usage

```clojure
(require '[ui.notification :as notification]
         '[ui.button :as button])

(notification/notification-stack {}
  (notification/notification {:status :success :title "Copied to clipboard"})
  (notification/notification {:status :loading
                              :title "Running in background"
                              :description [:code "bb test --focus xi.session-test"]
                              :meta "0:12"
                              :on-dismiss dismiss!}
    (button/button {:size :sm :on-click open-terminal!} "Open")))
```

## Examples

### A background command that finished

```clojure
(notification/notification {:status :success
                            :title "Tests passed"
                            :description [:code "bb test"]
                            :meta "212 tests · 34 s"}
  (button/button {:size :sm :on-click open-terminal!} "Open"))
```

### A failure that stays until dismissed

```clojure
(notification/notification {:status :danger
                            :title "Command failed"
                            :description [:code "bb test"]
                            :meta "exit 1 · 1 failure"
                            :on-dismiss dismiss!}
  (button/button {:size :sm :on-click open-terminal!} "Open"))
```

### Pinned to the bottom right of the viewport, fanned out on tap

```clojure
(notification/notification-stack {:position :bottom-right :expanded @expanded?}
  (for [n notifications] (notification/notification n)))
```

## API reference

### `notification`

One card: a tinted icon tile, a title with an optional description and meta line, actions at the end and an optional dismiss button on its top-left corner. role status, or alert when :danger.

Children: Actions: one small button, like Open or Undo.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:status` | `:loading \| :success \| :warning \| :danger` | – | nil is neutral. :loading shows a spinner in the tile; changing it swaps the icon in place. |
| `:icon` | `icon keyword \| false` | – | Overrides the status's icon (:check, :alert-triangle, :x); false removes the tile. |
| `:title` | `string \| hiccup` | – | What happened, one line, truncated. |
| `:description` | `string \| hiccup` | – | Second line, truncated: the command, the file. |
| `:meta` | `string \| hiccup` | – | Muted third line with tabular numbers: elapsed time, counts. |
| `:on-dismiss` | `fn` | – | Shows the dismiss button and handles its click (:cljs, :squint). |
| `:dismiss-attrs` | `map` | – | Shows the dismiss button with these attributes, e.g. HTMX ones on :clj. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `notification-stack`

A polite live region, newest child first: the first sits in front, the next two tuck behind it as edges 12px apart, the rest hide. Hover, keyboard focus or :expanded fans them out into a list with the newest at the bottom.

Children: notification elements, newest first.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:expanded` | `boolean` | – | Fans the stack out; set it on tap, since touch screens have no hover. |
| `:position` | `:bottom-right \| :bottom-center \| :bottom-left` | – | Pins the stack to that viewport edge; full width on phones. nil keeps it in the flow. |
| `:label` | `string` | `"Notifications"` | Accessible name of the region. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `notification-classes`

Class string for {:status :multiline}.

### `notification-class-list`

Vector form of notification-classes.

### `notification-stack-classes`

Class string for {:expanded :position}.

### `notification-stack-class-list`

Vector form of notification-stack-classes.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Tab | Moves into the stack, which fans out while it holds focus, then between actions and dismiss buttons. |

## Accessibility

- The stack is a labelled section with aria-live polite, so new cards are announced; a :danger card is role alert.
- Tucked cards keep their buttons in the tab order; focusing one fans the stack out so it is visible.
- The dismiss button is labelled Dismiss and has a 44px tap target around its 20px circle.

## Motion

- A new card rises from below over --duration-standard with --ease-enter; older ones slide back and narrow by 0.05 per step.
- A status change pops the new icon in with --ease-spring. Fanning out slides the tucked cards up into the list.
- Reduced motion keeps the end states and drops the travel.

## Responsive behavior

- The stack is at most 352px wide. Pinned stacks span the screen with a 16px gutter below 480px.
- Touch screens show the dismiss button all the time; pointers reveal it on hover or focus.

## Targets

- **Hiccup (`:clj`)**: Static markup; give :dismiss-attrs HTMX attributes to dismiss on the server.
- **Replicant (`:cljs`)**: :on-dismiss becomes {:on {:click f}}. Give each card a stable :replicant/key in :attrs so a status change updates it in place.
- **Squint (`:squint`)**: :on-dismiss is a flat :on-click attribute.

## Notes for AI

- The host owns the queue: it renders the cards newest first, removes them when their time is up and sets :expanded on tap.
- Copy is a label in words: Copied to clipboard, Tests passed, Command failed. Not Success!
- Background work keeps one card and changes its :status from :loading to :success or :danger instead of adding a second card.
- Keep a failure until it is dismissed; let successes leave after a few seconds.

## Related

- [Toast](toast.md): A brief floating confirmation for completed background work, shown by the JS runtime on every target.
- [Alert](alert.md): A persistent inline message with a tone, icon and optional title that helps people recover or continue.
- [Processing bar](processing-bar.md): A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.
- [Spinner](spinner.md): An indeterminate loading indicator in four sizes: a stretching accent arc on a muted track.
- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.

## Guidance for AI tools

Notification: Cards for things that happen out of view, like a copy or a command running in the background, stacked with the newest in front and the rest tucked behind until you reach for them. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
