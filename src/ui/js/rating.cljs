(ns ui.js.rating
  "Interactivity runtime for [data-ui-rating] elements (ui.rating).

   The rating is a radio group whose stars follow the checked or hovered
   radio in CSS, so picking a value needs no JS. Document-level listeners
   add the rest:

     - choosing the checked value again (click or tap on its half) clears
       the rating: the hidden `0` radio gets checked
     - Backspace / Delete on a focused radio clears it too
     - a change sets the wrapper's data-value and pops the stars
       (.rating--committed, removed when the animation ends)

   A clear dispatches native `input` and `change` events on the `0` radio,
   so forms, htmx triggers and squint/replicant :on-change see it like any
   other pick.")

(def ^:private pressed
  ;; The radio that was already checked when the pointer went down on its
  ;; half: the click that follows clears instead of re-picking it.
  (atom nil))

(defn- closest [el sel]
  (when (and el (.-closest el)) (.closest el sel)))

(defn- root-of [el] (closest el "[data-ui-rating]"))

(defn- disabled? [root]
  (.contains (.-classList root) "rating--disabled"))

(defn- commit! [root value]
  (.setAttribute root "data-value" value)
  (.remove (.-classList root) "rating--committed")
  (when (not= value "0")
    ;; Reading offsetWidth restarts the animation when it is still running.
    (.-offsetWidth root)
    (.add (.-classList root) "rating--committed")))

(defn- clear! [root]
  (when-let [none (.querySelector root ".rating-input-none")]
    (set! (.-checked none) true)
    (.dispatchEvent none (js/Event. "input" #js {:bubbles true}))
    (.dispatchEvent none (js/Event. "change" #js {:bubbles true}))))

(defn- on-pointerdown [e]
  (reset! pressed
          (when-let [hit (closest (.-target e) ".rating-hit")]
            (let [input (.querySelector hit ".rating-input")]
              (when (and input (.-checked input)) input)))))

(defn- on-click [e]
  (when-let [hit (closest (.-target e) ".rating-hit")]
    (let [root (root-of hit)
          input (.querySelector hit ".rating-input")]
      (when (and root input (not (disabled? root)) (= input @pressed))
        ;; The label's click would check the radio again; clear instead.
        (.preventDefault e)
        (reset! pressed nil)
        (clear! root)))))

(defn- on-change [e]
  (let [t (.-target e)]
    (when (and t (.-classList t) (.contains (.-classList t) "rating-input"))
      (when-let [root (root-of t)]
        (commit! root (.-value t))))))

(defn- on-keydown [e]
  (let [t (.-target e)
        k (.-key e)]
    (when (and t (.-classList t) (.contains (.-classList t) "rating-input")
               (or (= k "Backspace") (= k "Delete")))
      (when-let [root (root-of t)]
        (when-not (disabled? root)
          (.preventDefault e)
          (clear! root))))))

(defn- on-animationend [e]
  ;; The pop is staggered: done when the last star's ends.
  (let [t (.-target e)]
    (when-let [root (root-of t)]
      (when (= t (some-> (.querySelector root ".rating-stars") .-lastElementChild))
        (.remove (.-classList root) "rating--committed")))))

(defn init! []
  (.addEventListener js/document "pointerdown" on-pointerdown true)
  (.addEventListener js/document "click" on-click true)
  (.addEventListener js/document "change" on-change)
  (.addEventListener js/document "keydown" on-keydown)
  (.addEventListener js/document "animationend" on-animationend))

(init!)
