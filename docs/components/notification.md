# Notification

> Sonner's toaster in this framework's card: toasts stacked per screen position, the newest in front and the rest tucked behind, fanned out on hover, swiped away, with a pure queue model and Sonner's toast() for pages without app state.

- Type: Component (feedback, Messages)
- Namespace: `ui.notification`
- Source: `src/ui/notification.cljc`
- Public fns: `notification-class-list`, `notification-classes`, `loading-locked?`, `swipable?`, `notification`, `position-parts`, `default-swipe-directions`, `hotkey-label`, `offset-attrs`, `notification-toaster`, `queue`, `lifetime`, `post`, `dismiss`, `dismiss-all`, `mute`, `unmute`, `muted?`, `pause`, `resume`, `expire`, `next-due`, `newly-closed`, `visible`, `active`
- JS runtime: `ui.js.notification` (bundled in ui-runtime.js)
- Keywords: notification, toast, toaster, sonner, toast stack, background job, copied, snackbar, swipe to dismiss, promise toast

## When to use

- Results of work that happens out of view: a command in the background, an export, a sync. A :loading toast turns into its result in place (post the same :id).
- A confirmation with an action, like Undo or Open.
- Several at once: the toaster keeps them to one toast's height until hovered, tapped or reached with Alt+T.

## When not to use

- Use toast for the legacy one-line flash the runtime shows on its own.
- Use alert for a message that belongs to the page and stays until resolved.
- Use processing-bar for a list of uploads or jobs watched as they progress.

## Usage

```clojure
(require '[ui.notification :as notification])

;; the queue lives in the host's state; every handler gets `now`
(defonce !q (atom (notification/queue)))

(defn notify! [item]
  (swap! !q notification/post item (js/Date.now))
  (schedule!)) ; one timer at (notification/next-due @!q) → notification/expire

(notification/notification-toaster
  {:items      (notification/visible @!q)
   :position   :bottom-right
   :on-dismiss (fn [id reason] (swap! !q notification/dismiss id (js/Date.now) reason) (schedule!))
   :on-pause   (fn [paused?] (swap! !q (if paused? notification/pause notification/resume) (js/Date.now)) (schedule!))})
```

## Examples

### A background job: loading, then its result in the same toast

```clojure
(notify! {:id "job-1" :type :loading :title "Running in background" :description [:code "bb test"]})
;; later
(notify! {:id "job-1" :type :success :title "Tests passed" :meta "212 tests · 34 s"
          :action {:label "Open" :on-click open-terminal!}})
```

### Sonner's toast() on a server-rendered page (needs ui-runtime.js)

```clojure
;; in a handler or an onclick attribute
window.__uiNotify.success("Saved", {description: "3 files", action: {label: "Undo", onClick: undo}})
window.__uiNotify.promise(fetch("/export"), {loading: "Exporting", success: "Export finished", error: "Export failed"})

;; or from an HTMX response fragment
[:div {:data-ui-notify "Saved" :data-type "success"}]
```

### Contained above a composer instead of the viewport

```clojure
(notification/notification-toaster {:items items :contained true :offset {:bottom 64 :right 16}})
```

## API reference

### `notification-toaster`

Sonner's Toaster: a polite live region (section) with one list per position, each toast an <li> carrying Sonner's data attributes. The ui.js.notification runtime shows and moves them; dismissals and pausing come back as notificationdismiss and notificationpause DOM events.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:items` | `vector` | – | The toasts, newest first: notification props plus :id, :position, :removed-at (the queue model's visible). |
| `:position` | `:top-left \| :top-center \| :top-right \| :bottom-left \| :bottom-center \| :bottom-right` | `:bottom-right` |  |
| `:expand` | `boolean` | – | Always fanned out. |
| `:visible-toasts` | `number` | `3` |  |
| `:gap` | `px` | `14` |  |
| `:offset` | `px \| CSS length \| {:top :right :bottom :left}` | `24px` |  |
| `:mobile-offset` | `same` | `16px` | Under 600px wide, where toasts span the width. |
| `:close-button` | `boolean` | – | Close buttons on every toast. |
| `:rich-colors` | `boolean` | – | Rich colors on every toast. |
| `:invert` | `boolean` | – | Every toast on the opposite theme's surface. |
| `:dir` | `"ltr" \| "rtl"` | – |  |
| `:hotkey` | `vector` | `["altKey" "KeyT"]` | Focuses and fans out the toaster; Escape folds it. |
| `:label` | `string` | `"Notifications"` | Accessible name; the hotkey is appended. |
| `:swipe-directions` | `vector of "top" "right" "bottom" "left"` | – | Default: toward the toaster's edge and side. |
| `:icons` | `map` | – | {:success :info :warning :error :loading} icon keyword or hiccup. |
| `:contained` | `boolean` | – | Positioned in the nearest positioned ancestor instead of the viewport (not in Sonner). |
| `:on-dismiss` | `fn` | – | (fn [id reason]): close, swipe, action or cancel (:cljs, :squint). |
| `:on-pause` | `fn` | – | (fn [paused?]): while hovered, pressed or the tab hidden. |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – | HTMX on :clj, e.g. hx-trigger on notificationdismiss. |

### `notification`

One card as a static block (previews, a page's own list). Toasts in a toaster are the same card with stacking added.

Children: Extra actions after :cancel and :action.

| Prop | Type | Default | Description |
| --- | --- | --- | --- |
| `:type` | `:default \| :success \| :info \| :warning \| :error \| :loading` | – | :loading shows Sonner's twelve-bar loader. |
| `:title` | `string \| hiccup` | – |  |
| `:description` | `string \| hiccup` | – | Second line, truncated. |
| `:meta` | `string \| hiccup` | – | Muted third line, tabular numbers (ours, not Sonner's). |
| `:icon` | `icon keyword \| hiccup \| false` | – | Overrides the type's icon; false for none. |
| `:action` | `{:label :on-click :attrs} \| hiccup` | – | Dismisses the toast after the click unless the event was prevented. |
| `:cancel` | `{:label :on-click :attrs} \| hiccup` | – | A quieter button before the action; dismisses. |
| `:close-button` | `boolean` | – | The round close button on the top-left corner. |
| `:close-label` | `string` | `"Close toast"` |  |
| `:dismissible` | `boolean` | `true` | false: close and swipe do nothing. true also closes and swipes a :loading toast, which otherwise stays until it settles (Sonner's rule). |
| `:rich-colors` | `boolean` | – | Tints the whole card with its type's color. |
| `:invert` | `boolean` | – |  |
| `:unstyled` | `boolean` | – | No card surface. |
| `:class-names` | `map` | – | :title :description :content :icon :close-button :action-button :cancel-button |
| `:class` | `string` | – |  |
| `:attrs` | `map` | – |  |

### `queue`

An empty queue: {:items [] :muted #{} :paused-at nil :seq 0}. Items are toast props plus :id, :source, :duration, :position, and what the model stamps (:expires-at, :removed-at, :closed).

### `post`

(post state item now) or with opts {:duration :limit}. A queued :id updates in place (a loading one gets :promise for its loader fade); a leaving one is replaced; else in front.

### `lifetime`

Sonner's rule: :loading and ##Inf stay, a positive :duration counts, else the default 4 s (errors too).

### `dismiss`

(dismiss state id now) or with a reason: the item stays remove-delay (200 ms) with :removed-at for its exit, then expire drops it.

### `dismiss-all`

(dismiss-all state now).

### `expire`

(expire state now): starts closing what is due (:closed :auto, not while paused) and drops what finished leaving.

### `next-due`

The one time to call expire next, nil for never.

### `pause`

(pause state now): Sonner pauses while expanded, pressed or the tab is hidden.

### `resume`

(resume state now): each clock keeps the time it had left.

### `newly-closed`

(newly-closed before after): items that started leaving, for onAutoClose (:closed :auto) and onDismiss.

### `visible`

Items to render, newest first, muted sources left out, leaving ones kept for their exit.

### `active`

Visible items that are not leaving.

### `mute`

(mute state source): queued and timed, not visible.

### `unmute`

(unmute state source).

### `muted?`

(muted? state source).

### `position-parts`

:bottom-right → ["bottom" "right"].

### `default-swipe-directions`

Toward the toaster's edge and side.

### `hotkey-label`

["altKey" "KeyT"] → "alt+T".

### `offset-attrs`

Offset prop → data-<prefix>-<side> attributes the runtime turns into CSS variables.

### `notification-classes`

Class string for {:type :multiline}.

### `notification-class-list`

Vector form of notification-classes.

## Keyboard interactions

| Keys | Action |
| --- | --- |
| Alt+T | Focuses the toaster and fans it out. |
| Escape | Folds it while focus is inside. |
| Tab | Moves between toasts and their buttons. |

## Accessibility

- The toaster is a labelled section with aria-live polite and aria-relevant additions text, so new and updated toasts are announced.
- Focus that leaves the toaster returns to where it came from, as in Sonner.
- The close button is labelled Close toast and has a 44px tap target around its 20px circle.

## Motion

- Sonner's timing: toasts enter from the edge over 400ms, older ones tuck behind 14px apart and 5% narrower a step, and move to their offsets when fanned out.
- Leaving: the front one slides out and fades over 200ms; a swiped one leaves in its direction. A promise toast fades its loader out and its icon in.
- Reduced motion keeps the end states and drops the travel.

## Responsive behavior

- Toasts are 356px wide; under 600px they span the screen at the mobile offsets (16px).
- Touch: a tap fans the stack out, a swipe toward the toaster's edge or side dismisses.

## Targets

- **Hiccup (`:clj`)**: Render notification-toaster with the items; listen for notificationdismiss with HTMX (hx-trigger) or use window.__uiNotify / [data-ui-notify] markers.
- **Replicant (`:cljs`)**: :on-dismiss and :on-pause become listeners for the runtime's events; toasts are keyed by :id.
- **Squint (`:squint`)**: Same, as flat :on-notificationdismiss attributes.

## Notes for AI

- Keep the queue model's map in the host's state and change it only through post, dismiss, expire, pause and resume; schedule one timer at next-due.
- Pass (visible state) as :items so leaving toasts stay for their exit animation.
- Background work keeps one toast: post the same :id with :loading, then :success or :error.
- Copy is a label in words: Copied to clipboard, Tests passed, Command failed. Not Success!

## Related

- [Toast](toast.md): A brief floating confirmation for completed background work, shown by the JS runtime on every target.
- [Alert](alert.md): A persistent inline message with a tone, icon and optional title that helps people recover or continue.
- [Processing bar](processing-bar.md): A compact accent-bordered panel listing background jobs in progress with thumbnails, labels and spinners.
- [Spinner](spinner.md): An indeterminate loading indicator in four sizes: a stretching accent arc on a muted track.
- [Button](button.md): A single action: neutral primary, quiet outlined states, a loud filled option, press feedback and a loading spinner.

## Guidance for AI tools

Notification: Sonner's toaster in this framework's card: toasts stacked per screen position, the newest in front and the rest tucked behind, fanned out on hover, swiped away, with a pure queue model and Sonner's toast() for pages without app state. Use only the documented props; `:class` and `:attrs` are for layout and extra attributes, never for restyling internals. The same call renders on all three targets; see Targets for the per-target event and class conventions. Keep keyboard access and both themes intact.

Full library index: docs/llms.txt
