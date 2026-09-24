(ns ui.js.number-field
  "Interactivity runtime for [data-ui-number-field] elements.

   The server-rendered field (hiccup) has no event handlers, and the
   squint/replicant components delegate stepper/wheel behaviour here too so
   the math lives in one place. Document-level listeners:

     - wheel over a field → step its <input> up/down (preventDefault so the
       page doesn't scroll)
     - click on a [data-ui-number-step] button → step by its dir (±1)

   After changing the value the runtime dispatches native `input` and
   `change` events on the <input>, so controlled squint/replicant components
   pick the change up through their own :on-change handler, and plain hiccup
   forms see a normal input event.")

(defn- num [v fallback]
  (let [n (js/parseFloat v)]
    (if (js/isNaN n) fallback n)))

(defn- closest-field [el]
  (when (and el (.-closest el))
    (.closest el "[data-ui-number-field]")))

(defn- disabled? [field]
  (.hasAttribute field "data-ui-number-disabled"))

(defn- field-input [field]
  (.querySelector field "input"))

(defn- round6 [n]
  (/ (js/Math.round (* n 1e6)) 1e6))

(defn- step-input! [input dir]
  (when (and input (not (.-disabled input)))
    (let [raw-step (num (.-step input) 1)
          step (if (pos? raw-step) raw-step 1)     ; step="any"/0 → 1
          cur  (num (.-value input) 0)
          min* (when (not= "" (.-min input)) (num (.-min input) nil))
          max* (when (not= "" (.-max input)) (num (.-max input) nil))
          next (round6 (+ cur (* dir step)))
          next (cond-> next
                 (some? min*) (js/Math.max min*)
                 (some? max*) (js/Math.min max*))]
      (set! (.-value input) (str next))
      (.dispatchEvent input (js/Event. "input" #js {:bubbles true}))
      (.dispatchEvent input (js/Event. "change" #js {:bubbles true})))))

(defn- on-wheel [e]
  (when-let [field (closest-field (.-target e))]
    (when-not (disabled? field)
      (when-let [input (field-input field)]
        (.preventDefault e)
        (step-input! input (if (< (.-deltaY e) 0) 1 -1))))))

(defn- on-click [e]
  (when-let [btn (and (.-target e)
                      (.-closest (.-target e))
                      (.closest (.-target e) "[data-ui-number-step]"))]
    (when-let [field (closest-field btn)]
      (when-not (disabled? field)
        (step-input! (field-input field)
                     (num (.getAttribute btn "data-ui-number-step") 0))))))

(defn init! []
  (.addEventListener js/document "wheel" on-wheel #js {:passive false})
  (.addEventListener js/document "click" on-click))

(init!)
