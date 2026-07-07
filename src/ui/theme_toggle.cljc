(ns ui.theme-toggle
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]))

;; ── Theme toggle ────────────────────────────────────────────────────
;; A segmented control with three options: light / auto / dark.
;; Renders inline SVG icons (sun, monitor, moon) with an active
;; indicator that slides to the selected mode.
;;
;; Requires the JS runtime (`__uiTheme`) for interactivity.
;; In server-rendered (CLJ) mode, emits the markup; the JS runtime
;; hydrates behavior on load.

(def ^:private modes
  [{:mode "light" :icon :sun   :label "Light theme"}
   {:mode "auto"  :icon :monitor :label "System theme"}
   {:mode "dark"  :icon :moon  :label "Dark theme"}])

(defn theme-toggle-class-list
  "Returns a vector of CSS class strings for the toggle container."
  [{:keys [size]}]
  (let [s (or (some-> size util/kw-name) "md")]
    (cond-> ["theme-toggle"]
      (not= s "md") (conj (str "theme-toggle-" s)))))

(defn theme-toggle-classes
  "Returns a space-joined class string."
  [opts]
  (str/join " " (theme-toggle-class-list opts)))

(defn theme-toggle
  "Render a segmented theme toggle (light / auto / dark).

   Props:
     :mode      - current mode: \"light\", \"auto\", or \"dark\" (default \"auto\")
     :on-change - callback receiving the new mode string
     :size      - :sm, :md (default)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [mode on-change size class attrs] :as _props}]
  (let [current (or mode "auto")]
    #?(:squint
       (let [classes (cond-> (theme-toggle-classes {:size size})
                       class (str " " class))]
         [:div (merge {:class classes :role "radiogroup" :aria-label "Theme"} attrs)
          (into [:<>]
                (map (fn [{:keys [mode icon label]}]
                       (let [active (= mode current)]
                         [:button {:class (if active "theme-toggle-btn theme-toggle-btn-active" "theme-toggle-btn")
                                   :role "radio"
                                   :aria-checked (str active)
                                   :aria-label label
                                   :title label
                                   :on-click (when on-change (fn [_] (on-change mode)))}
                          (icon/icon {:icon-name icon :size :sm})]))
                     modes))])

       :cljs
       (let [cls (theme-toggle-class-list {:size size})
             classes (cond-> cls class (conj class))]
         (into [:div (merge {:class classes :role "radiogroup" :aria-label "Theme"} attrs)]
               (map (fn [{:keys [mode icon label]}]
                      (let [active (= mode current)]
                        [:button (cond-> {:class (if active
                                                   ["theme-toggle-btn" "theme-toggle-btn-active"]
                                                   ["theme-toggle-btn"])
                                          :role "radio"
                                          :aria-checked (str active)
                                          :aria-label label
                                          :title label}
                                   on-change (assoc :on {:click (fn [_] (on-change mode))}))
                         (icon/icon {:icon-name icon :size :sm})]))
                    modes)))

       :clj
       (let [classes (cond-> (theme-toggle-classes {:size size})
                       class (str " " class))]
         [:div (merge {:class classes :role "radiogroup" :aria-label "Theme"} attrs)
          (for [{:keys [mode icon label]} modes]
            (let [active (= mode current)]
              [:button {:class (str "theme-toggle-btn" (when active " theme-toggle-btn-active"))
                        :role "radio"
                        :aria-checked (str active)
                        :aria-label label
                        :title label}
               (icon/icon {:icon-name icon :size :sm})]))]))))
