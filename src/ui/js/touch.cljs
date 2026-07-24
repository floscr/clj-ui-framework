(ns touch
  "Touch/mobile runtime — detection plus mobile hardening.

   1. Applies `.clj-ui-touch` to <html> on touch devices (activates
      the framework's touch-friendly alternatives, see AGENTS.md
      \"Touch Alternatives\"). Detection uses
      matchMedia(\"(hover: none)\") — the primary input can't hover —
      which mirrors the `@media (hover: hover)` gating in component
      CSS. Kept in sync when the media query changes (e.g. attaching
      a mouse to a tablet).

   2. On touch devices, hardens the page against zoom gestures:
      - rewrites (or creates) the viewport meta with
        maximum-scale=1 / user-scalable=no / viewport-fit=cover
        (also prevents the iOS zoom-on-input-focus)
      - blocks the iOS Safari pinch gesture (gesturestart), which
        ignores user-scalable=no

      Double-tap zoom on interactive elements is disabled separately
      in CSS via touch-action: manipulation (ui/mobile.css).

   Loaded automatically via ui-runtime.js (hiccup apps) or a
   side-effect require of [ui.js.touch] (squint/replicant SPAs).

   Imperative API: window.__uiTouch() re-syncs the class and returns
   whether touch mode is active.")

(def ^:private mq (js/window.matchMedia "(hover: none)"))

(def ^:private viewport-content
  "width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover")

(defn- harden-viewport! []
  (if-let [meta-el (js/document.querySelector "meta[name=viewport]")]
    (.setAttribute meta-el "content" viewport-content)
    (let [m (js/document.createElement "meta")]
      (.setAttribute m "name" "viewport")
      (.setAttribute m "content" viewport-content)
      (.appendChild (.-head js/document) m))))

(defn- sync! []
  (let [touch? (.-matches mq)
        cl (.-classList (.-documentElement js/document))]
    (if touch?
      (.add cl "clj-ui-touch")
      (.remove cl "clj-ui-touch"))
    touch?))

(aset js/window "__uiTouch" sync!)

;; documentElement exists as soon as any script runs — apply immediately
;; (before first paint) to avoid a flash of hover-dependent UI.
(when (sync!)
  (harden-viewport!)
  ;; iOS Safari ignores user-scalable=no for pinch — block the gesture.
  ;; Desktop trackpad pinch never reaches here (touch mode only).
  (.addEventListener js/document "gesturestart"
                     (fn [e] (.preventDefault e))))
(.addEventListener mq "change" sync!)
