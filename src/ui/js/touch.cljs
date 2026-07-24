(ns touch
  "Touch-device detection — applies `.clj-ui-touch` to <html>.

   Activates the framework's touch-friendly alternatives (see AGENTS.md
   \"Touch Alternatives\") automatically wherever the JS runtime is
   loaded, so apps no longer need their own detection snippet.

   Detection uses matchMedia(\"(hover: none)\") — the primary input
   can't hover — which mirrors the `@media (hover: hover)` gating used
   by the component CSS. The class is kept in sync when the media query
   changes (e.g. attaching a mouse to a tablet).

   Imperative API: window.__uiTouch() re-syncs the class and returns
   whether touch mode is active.")

(def ^:private mq (js/window.matchMedia "(hover: none)"))

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
(sync!)
(.addEventListener mq "change" sync!)
