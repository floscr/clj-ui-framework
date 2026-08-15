(ns ui.js.select
  "Tiny JS runtime for the custom select component. Compiled from squint,
   loaded as a <script> tag in the hiccup target and reused by
   squint/replicant via window.__uiSelect.

   Opens a floating listbox anchored to the trigger button, handles
   dismiss on outside-tap / Escape / scroll, keyboard navigation, and
   selection. Like the context menu, an outside tap is *consumed*
   (preventDefault + stopPropagation) so it only closes the listbox
   instead of activating whatever is underneath.

   Value handling:
   - :clj (hiccup) — uncontrolled: the runtime updates the trigger label,
     data-select-value and the sibling hidden <input>, then dispatches a
     'change' event on that input.
   - :squint/:cljs — controlled: an on-change callback is passed with the
     picked value; the framework re-renders the trigger.")

;; ── State (stored on window to avoid module scope issues) ───────────

(defn- get-state []
  (or (aget js/window "__uiSelectState")
      (let [s #js {:menu nil :trigger nil :cleanup nil}]
        (aset js/window "__uiSelectState" s)
        s)))

;; ── Dismiss ─────────────────────────────────────────────────────────

(defn- dismiss! []
  (let [state (get-state)]
    (when (.-menu state)
      (.remove (.-menu state))
      (set! (.-menu state) nil))
    (when (.-trigger state)
      (.setAttribute (.-trigger state) "aria-expanded" "false")
      (set! (.-trigger state) nil))
    (when (.-cleanup state)
      ((.-cleanup state))
      (set! (.-cleanup state) nil))))

;; ── Selection ───────────────────────────────────────────────────────

(defn- update-trigger! [trigger value label]
  ;; :clj / uncontrolled path — reflect the pick into the DOM.
  (set! (.. trigger -dataset -selectValue) value)
  (let [span (.querySelector trigger ".select-value")]
    (when span
      (set! (.-textContent span) label)
      (.remove (.-classList span) "select-value--placeholder")))
  (let [wrap  (.-parentElement trigger)
        input (when wrap (.querySelector wrap "input[type=hidden]"))]
    (when input
      (set! (.-value input) value)
      (.dispatchEvent input (js/Event. "change" #js {:bubbles true})))))

;; ── Menu DOM Creation ───────────────────────────────────────────────

(defn- create-menu [options current-value on-change trigger]
  (let [menu (.createElement js/document "div")]
    (set! (.-className menu) "select-menu")
    (.setAttribute menu "role" "listbox")
    (.forEach options
      (fn [opt]
        (let [value    (aget opt "value")
              label    (aget opt "label")
              selected (= value current-value)
              el       (.createElement js/document "button")]
          (set! (.-className el)
                (if selected "select-option select-option--selected" "select-option"))
          (.setAttribute el "type" "button")
          (.setAttribute el "role" "option")
          (.setAttribute el "tabindex" "-1")
          (.setAttribute el "aria-selected" (if selected "true" "false"))
          (set! (.-textContent el) label)
          (.addEventListener el "click"
            (fn [e]
              (.preventDefault e)
              (.stopPropagation e)
              (if on-change
                (do (dismiss!) (on-change value))
                (do (update-trigger! trigger value label) (dismiss!)))))
          (.appendChild menu el))))
    menu))

;; ── Positioning ─────────────────────────────────────────────────────

(defn- position-menu! [menu trigger]
  (let [r  (.getBoundingClientRect trigger)]
    (set! (.. menu -style -minWidth) (str (.-width r) "px"))
    (.appendChild js/document.body menu)
    (let [mr    (.getBoundingClientRect menu)
          vw    (.-innerWidth js/window)
          vh    (.-innerHeight js/window)
          left  (js/Math.max 8 (js/Math.min (.-left r) (- vw (.-width mr) 8)))
          below (+ (.-bottom r) 4)
          top   (if (> (+ below (.-height mr)) vh)
                  (js/Math.max 8 (- (.-top r) (.-height mr) 4))
                  below)]
      (set! (.. menu -style -left) (str left "px"))
      (set! (.. menu -style -top)  (str top "px")))))

;; ── Keyboard Navigation ─────────────────────────────────────────────

(defn- focus-selected-or-first! [menu]
  (let [sel (.querySelector menu ".select-option--selected")]
    (if sel
      (.focus sel)
      (let [first-opt (.querySelector menu ".select-option")]
        (when first-opt (.focus first-opt))))))

(defn- focus-next! [menu direction]
  (let [items  (.querySelectorAll menu ".select-option")
        active (.-activeElement js/document)
        len    (.-length items)]
    (when (> len 0)
      (let [current-idx (let [result (atom -1)]
                          (.forEach items
                            (fn [item i]
                              (when (= item active) (reset! result i))))
                          @result)
            next-idx    (cond
                          (= direction "down") (if (< current-idx (- len 1))
                                                 (+ current-idx 1) 0)
                          (= direction "up")   (if (> current-idx 0)
                                                 (- current-idx 1) (- len 1))
                          :else current-idx)]
        (.focus (aget items next-idx))))))

;; ── Open ────────────────────────────────────────────────────────────

(defn open-select
  "Open a select listbox anchored to its trigger button.
   trigger   - the trigger button element
   options   - (optional) JS array of {value, label}. If nil, reads the
               trigger's data-select-options attribute (:clj path).
   on-change - (optional) callback (fn [value]). When provided
               (:squint/:cljs), the runtime does not mutate the trigger."
  [trigger & args]
  ;; Clicking the trigger while open is consumed by the outside-click
  ;; handler (which closes the menu), so reaching here always means open.
  (dismiss!)
  (let [options   (let [passed (first args)]
                    (if passed
                      passed
                      (let [json (.. trigger -dataset -selectOptions)]
                        (when json (js/JSON.parse json)))))
        on-change (second args)
        current   (.. trigger -dataset -selectValue)]
    (when options
      (let [menu  (create-menu options current on-change trigger)
            state (get-state)]
        (set! (.-menu state) menu)
        (set! (.-trigger state) trigger)
        (.setAttribute trigger "aria-expanded" "true")
        (position-menu! menu trigger)
        (focus-selected-or-first! menu)
        (let [on-click  (fn [e]
                          (when (not (.contains menu (.-target e)))
                            ;; consume the tap: only close the listbox, don't
                            ;; activate whatever is underneath (incl. the
                            ;; trigger — which gives toggle-to-close)
                            (.preventDefault e)
                            (.stopPropagation e)
                            (dismiss!)))
              on-key    (fn [e]
                          (let [key (.-key e)]
                            (cond
                              (= key "Escape")
                              (do (.preventDefault e) (dismiss!) (.focus trigger))
                              (= key "ArrowDown")
                              (do (.preventDefault e) (focus-next! menu "down"))
                              (= key "ArrowUp")
                              (do (.preventDefault e) (focus-next! menu "up"))
                              (= key "Enter")
                              (let [active (.-activeElement js/document)]
                                (when (and active (.contains menu active))
                                  (.preventDefault e)
                                  (.click active)))
                              (= key "Tab")
                              (dismiss!))))
              on-scroll (fn [_] (dismiss!))
              on-resize (fn [_] (dismiss!))
              cleanup   (fn []
                          (.removeEventListener js/document "click" on-click true)
                          (.removeEventListener js/document "keydown" on-key true)
                          (.removeEventListener js/window "scroll" on-scroll true)
                          (.removeEventListener js/window "resize" on-resize))]
          (.addEventListener js/document "click" on-click true)
          (.addEventListener js/document "keydown" on-key true)
          (.addEventListener js/window "scroll" on-scroll true)
          (.addEventListener js/window "resize" on-resize)
          (set! (.-cleanup state) cleanup))))))

;; ── Attach to window ────────────────────────────────────────────────

(aset js/window "__uiSelect" open-select)
