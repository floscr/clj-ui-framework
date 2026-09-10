(ns ui.panels
  "Resizable panel groups — a port of motion-panels
   (https://motion-panels.letstri.dev) for all three targets.

   Renders data-attributed markup; all behavior (drag, keyboard,
   collapse, animation) lives in the shared JS runtime
   (ui.js.panels, bundled in ui-runtime.js). Squint/Replicant SPAs
   that don't load ui-runtime.js need a side-effect require of the
   runtime module.

   Usage:

     (panels/group {:orientation :horizontal}
       (panels/panel {:size 240 :min-size 160 :max-size 420
                      :collapsible true :persist-key \"sidebar\"}
         sidebar-content)
       (panels/separator {})
       (panels/panel {:pin true}
         main-content))

   A panel with a :size (px number or percent string like \"30%\") is
   resizable; a panel without one fills the remaining space. Without a
   separator, a sized panel is draggable by the edge facing the fill.
   :pin on the fill panel keeps its content anchored (no reflow) while
   a sized panel collapses.

   Size changes arrive as bubbling `ui-panels-resize` CustomEvents on
   the panel element (detail {size pixels}); collapse toggles as
   `ui-panels-collapse` (detail {collapsed}). Controlled usage: change
   data-size / toggle data-collapsed and the runtime animates to it.
   :persist-key saves size+collapsed to localStorage automatically."
  (:require [clojure.string :as str]
            [ui.util :as util]))

;; ── Pure attribute/class builders (shared, testable) ────────────────

(defn size-str
  "Coerce a size prop (number or string like \"30%\") to its attr string."
  [s]
  (str s))

(defn group-attrs
  "Data attributes for a panel group."
  [{:keys [orientation]}]
  {:data-ui-panels-group "true"
   :data-orientation (if orientation (util/kw-name orientation) "horizontal")})

(defn panel-attrs
  "Data attributes for a sized panel."
  [{:keys [size min-size max-size default-size collapsible collapsed
           persist-key]}]
  (cond-> {:data-ui-panels-panel "true"
           :data-size (size-str size)}
    min-size (assoc :data-min-size (size-str min-size))
    max-size (assoc :data-max-size (size-str max-size))
    default-size (assoc :data-default-size (size-str default-size))
    collapsible (assoc :data-collapsible "true")
    collapsed (assoc :data-collapsed "true")
    persist-key (assoc :data-persist-key persist-key)))

(defn- cls
  "Base class tokens + optional user class, in the target's format."
  [bases class]
  #?(:squint (str/join " " (if class (conj bases class) bases))
     :cljs (cond-> (vec bases) class (util/conj-classes class))
     :clj (str/join " " (if class (conj bases class) bases))))

;; ── Components ──────────────────────────────────────────────────────

(defn group
  "Flex container for panels. Opts: :orientation (:horizontal default,
   :vertical), :class, :attrs."
  [{:keys [orientation class attrs]} & children]
  (into [:div (merge {:class (cls ["ui-panels-group"] class)}
                     (group-attrs {:orientation orientation})
                     attrs)]
        children))

(defn separator
  "Draggable, keyboard-operable seam between two panels. Opts: :label
   (aria-label), :class, :attrs."
  [& [{:keys [label class attrs]}]]
  [:div {:class (cls ["ui-panels-separator"] class)
         :data-ui-panels-separator "true"}
   [:div (merge {:class (cls ["ui-panels-grip"] nil)
                 :role "separator"
                 :tabindex "0"
                 :aria-label (or label "Resize panel")}
                attrs)]])

(defn panel
  "A panel. With :size (px number or \"NN%\") it holds that size and is
   resizable; without, it fills the remaining space. Opts (sized):
   :min-size :max-size :default-size :collapsible :collapsed
   :persist-key. Opts (fill): :pin. Common: :class :attrs."
  [{:keys [size pin class attrs] :as props} & children]
  (if (nil? size)
    (let [base (merge {:class (cls ["ui-panels-fill"] class)
                       :data-ui-panels-fill "true"}
                      (when pin {:data-pin "true"})
                      attrs)]
      (if pin
        [:div base
         (into [:div {:class (cls ["ui-panels-fill-inner"] nil)}] children)]
        (into [:div base] children)))
    [:div (merge {:class (cls ["ui-panels-panel"] class)}
                 (panel-attrs props)
                 attrs)
     (into [:div {:class (cls ["ui-panels-content"] nil)}] children)
     [:div {:class (cls ["ui-panels-grip" "ui-panels-edge"] nil)
            :role "separator"
            :tabindex "0"
            :aria-label "Resize panel"}]]))
