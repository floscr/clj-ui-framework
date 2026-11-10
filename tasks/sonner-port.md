# Port Sonner to ui.notification

Source read: sonner `src/index.tsx`, `src/styles.css`, `src/types.ts`, `src/state.ts`
(main, 10 Nov 2026). Values below are Sonner's.

## Split

Sonner keeps everything in React state. Here it splits along what each target can do:

| Layer | Where | Owns |
|---|---|---|
| Model (pure cljc) | `ui.notification` | queue, ids, types, durations, two-step removal, pause, mute, positions |
| Markup (cljc, all targets) | `ui.notification` | toaster `section` > `ol` per position > `li` per toast, Sonner's data attributes and props |
| CSS (tokens) | `notification.css` | positions, offsets, lift, stacking, enter/exit, swipe-out keyframes, mobile, rich colors, invert |
| Runtime (squint, DOM only) | `ui.js.notification` | measuring heights, mount flag, hover expand, swipe, hotkey, focus restore, tab hidden; reports back through DOM events |

The runtime only sets attributes the markup never renders (`data-mounted`, `data-expanded`,
`data-swiping`, `--offset`, ...), so a Replicant/Squint re-render never fights it.

## 1. Model

- [ ] Types `:default :success :info :warning :error :loading` (keep `:danger` as alias of `:error`)
- [ ] Default lifetime 4000 for every type; `:loading` and `:duration ##Inf`/0 never expire
      (today: success 5 s, warning 8 s, danger stays; Sonner's error leaves after 4 s)
- [ ] Two-step removal: `dismiss` / `expire` mark `:removed-at now`; the item stays rendered
      (`data-removed`) and is dropped 200 ms later (`expire` + `next-due` cover both steps)
- [ ] `:dismissible false`: no close button, no swipe, `dismiss` refuses it
- [ ] Per-item `:position`, toaster default; `visible` grouped by position
- [ ] `:auto-closed` vs dismissed recorded so the host can fire `on-auto-close` / `on-dismiss`
- [ ] Keep: update by id, pause/resume (expanded, interacting, tab hidden), mute, limit
- [ ] Tests for each

## 2. Markup + props

Toaster (`notification-toaster`): `:position` (6), `:expand`, `:visible-toasts` 3, `:gap` 14,
`:offset` / `:mobile-offset` (number, string or per side), `:close-button`, `:rich-colors`,
`:invert`, `:dir`, `:hotkey` (label in aria: "Notifications alt+T"), `:label`,
`:swipe-directions`, `:icons`, `:contained` (ours, not Sonner: absolute in the nearest
positioned ancestor, what xi needs above the composer).

Toast (`notification`): `:id :type :title :description :icon` (nil hides it), `:action`
and `:cancel` `{:label :on-click}` or hiccup, `:close-button :dismissible :rich-colors
:invert :unstyled :position :class :class-names :style :on-dismiss`.

- [ ] Data attributes as Sonner: `data-type data-front data-index data-visible data-removed
      data-rich-colors data-invert data-styled data-dismissible data-y-position data-x-position`,
      vars `--index --toasts-before --z-index`
- [ ] Sonner's loader (12 bars) as the loading icon, fading into the result icon (promise look)

## 3. CSS (port `styles.css` onto tokens)

- [ ] Toaster fixed at offsets (24 px, 16 px under 600 px wide; full width on mobile)
- [ ] Enter: `translateY(±100%)`, opacity 0 → `data-mounted`
- [ ] Collapsed: back toasts `height: var(--front-toast-height)`,
      `translateY(lift * gap * index) scale(1 - 0.05 * index)`, children hidden; beyond 3 hidden
- [ ] Expanded: `translateY(lift * --offset)`, own height; `::after` bridge over the gap
- [ ] Exit: front up/down 100% + fade; collapsed back ones `translateY(40%)`, 500 ms / 200 ms
- [ ] Swipe: follows `--swipe-amount-x/y`; `swipe-out-{left,right,up,down}` 200 ms ease-out
- [ ] Transitions 400 ms transform/opacity/height (motion tokens where they match)
- [ ] Rich colors and invert on our tokens; close button top-left `translate(-35%,-35%)`, RTL mirrored
- [ ] Reduced motion: no transitions/animations

## 4. Runtime `ui.js.notification` (squint, in ui-runtime.js)

- [ ] Heights: ResizeObserver per toast → `--initial-height`, `--offset` (heights before + gap
      * index), `--front-toast-height` on the list; removed toasts keep their offset
- [ ] Mount: `data-mounted` the frame after insertion
- [ ] Expand on mouseenter/mousemove, collapse on leave unless a pointer is down; `expand` default
- [ ] Swipe: pointer capture, direction lock after 1 px, damping `1/(1.5+|d|/20)` off-axis,
      dismiss at 45 px or 0.11 px/ms in an allowed direction; ignores buttons and text selection
- [ ] Alt+T focuses and expands, Escape collapses, focus returns where it came from
- [ ] Emits DOM events on the toast/list: `notification-dismiss {id, reason swipe|close|action}`,
      `notification-expand {expanded}`, `notification-interact {on}`, `notification-visibility {hidden}`
- [ ] Tests for the pure parts (offset math, swipe decision, damping)

## 5. HTMX / hiccup: Sonner's imperative API

- [ ] `window.__uiNotify` / `toast.success|error|info|warning|loading|promise|dismiss` on the
      model in the runtime, rendering with the same markup; `toast-flash` markers feed it
- [ ] `ui.toast` becomes a thin alias, or stays as is (decide)

## 6. Docs, examples, verify

- [ ] Sidecar, examples (positions, rich colors, action/cancel, promise, swipe), gen-docs
- [ ] Side-by-side screenshots against sonner.emilkowal.ski (collapsed, expanded, exit, swipe)
- [ ] `bb build-theme` `bb lint-css` `bb test` `bb gen-docs` `bb check-dev`

## 7. xi

- [ ] Switch `xi.web.notifications` to the toaster (`:contained` above the composer), listen to
      the runtime's events (dismiss, expand → pause/resume, tab hidden), swipe on the phone
- [ ] Push framework, bump pin, restart, verify on the demo

## Decisions (10 Nov 2026)

1. Skin: ours (tinted icon tile, description, meta line), without the colored status border.
2. Everything else like Sonner: 4 s for every type, errors included; close button opt-in.
3. Phase 5 (imperative API) is in.

API follows Sonner: `:type` with `:default :success :info :warning :error :loading`
(replaces `:status`/`:danger`); the toaster renders items (data) instead of card children,
since index, front and visibility come from the list. Close, action and cancel clicks and
swipes are handled by the runtime, which tells the host through one `notification-dismiss`
event; pausing (expanded, pointer down, tab hidden) through `notification-pause`.
