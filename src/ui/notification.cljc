(ns ui.notification
  "Notifications: a card for something that happened or is happening
   out of view (copied, a command running in the background, its
   result), and a stack that tucks older cards behind the newest one
   and fans them out on hover, focus or :expanded.

   Both are plain markup the host renders; queueing, timers and exits
   belong to the host (ui.toast is the runtime-owned one-line flash)."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]
            [ui.spinner :as spinner]))

(def ^:private status-icons
  {"success" :check
   "warning" :alert-triangle
   "danger"  :x})

(defn notification-class-list
  "Classes for a notification. :status is :loading, :success, :warning or
   :danger (nil is neutral); :multiline aligns the icon with the first line
   when a description or meta follows the title."
  [{:keys [status multiline]}]
  (cond-> ["notification"]
    status    (conj (str "notification-" (util/kw-name status)))
    multiline (conj "notification-multiline")))

(defn notification-classes
  "Space-joined form of notification-class-list."
  [opts]
  (str/join " " (notification-class-list opts)))

(defn- leading-el
  "The tinted icon tile: a spinner while :loading, else :icon or the
   status's default icon. false suppresses it."
  [status icon-name]
  (let [s (some-> status util/kw-name)]
    (cond
      (false? icon-name) nil
      (= s "loading")    [:span {:class "notification-icon"} (spinner/spinner {:size :xs :attrs {:aria-hidden "true"}})]
      :else              (when-let [n (or icon-name (get status-icons s))]
                           [:span {:class "notification-icon"} (icon/icon {:icon-name n :size :sm})]))))

(defn- dismiss-el [on-dismiss dismiss-attrs]
  [:button (merge #?(:squint (cond-> {:class "notification-dismiss" :type "button" :aria-label "Dismiss"}
                               on-dismiss (assoc :on-click on-dismiss))
                     :cljs   (cond-> {:class ["notification-dismiss"] :type "button" :aria-label "Dismiss"}
                               on-dismiss (assoc-in [:on :click] on-dismiss))
                     :clj    {:class "notification-dismiss" :type "button" :aria-label "Dismiss"})
                  dismiss-attrs)
   (icon/icon {:icon-name :x :size :sm})])

(defn notification
  "One notification card.

   Props:
     :status        - :loading (spinner), :success, :warning, :danger; nil is neutral
     :icon          - icon keyword for the tile, overriding the status's;
                      false for none
     :title         - what happened, one line (string or hiccup)
     :description   - optional second line, truncated: the command, the file
     :meta          - optional muted third line: elapsed time, counts
     :on-dismiss    - click handler for the dismiss button (:cljs, :squint)
     :dismiss-attrs - attributes for the dismiss button (HTMX on :clj)
     :class         - additional CSS classes
     :attrs         - additional HTML attributes map

   The dismiss button shows when :on-dismiss or :dismiss-attrs is given.
   Children are actions at the end: one small button, like Open or Undo."
  [{:keys [status icon title description meta on-dismiss dismiss-attrs class attrs] :as _props} & actions]
  (let [actions (remove nil? actions)
        role    (if (= "danger" (some-> status util/kw-name)) "alert" "status")
        leading (leading-el status icon)]
    (cond-> [:div (util/class-attrs (notification-class-list {:status status
                                                              :multiline (boolean (or description meta))})
                                    class
                                    (merge {:role role} attrs))]
      leading       (conj leading)
      true          (conj (cond-> [:div {:class "notification-text"}
                                   [:p {:class "notification-title"} title]]
                            description (conj [:div {:class "notification-description"} description])
                            meta        (conj [:div {:class "notification-meta"} meta])))
      (seq actions) (conj (into [:div {:class "notification-actions"}] actions))
      (or on-dismiss dismiss-attrs) (conj (dismiss-el on-dismiss dismiss-attrs)))))

(defn notification-stack-class-list
  "Classes for a stack. :expanded fans it out; :position pins it to a
   viewport edge (:bottom-right, :bottom-center, :bottom-left)."
  [{:keys [expanded position]}]
  (cond-> ["notification-stack"]
    expanded (conj "notification-stack-expanded")
    position (conj (str "notification-stack-" (util/kw-name position)))))

(defn notification-stack-classes
  "Space-joined form of notification-stack-class-list."
  [opts]
  (str/join " " (notification-stack-class-list opts)))

(defn notification-stack
  "A polite live region that stacks notifications, newest first: the first
   child sits in front, the next two tuck behind it as edges, the rest
   hide. Hover, keyboard focus or :expanded fans them out into a list.

   Props:
     :expanded - boolean; fan out (set it on tap for touch screens)
     :position - nil stays in the flow; :bottom-right, :bottom-center or
                 :bottom-left pins it to that viewport edge
     :label    - accessible name of the region (default \"Notifications\")
     :class    - additional CSS classes
     :attrs    - additional HTML attributes map

   Children are notification elements, newest first."
  [{:keys [expanded position label class attrs] :as _props} & notifications]
  (into [:section (util/class-attrs (notification-stack-class-list {:expanded expanded :position position})
                                    class
                                    (merge {:aria-label (or label "Notifications")
                                            :aria-live "polite"}
                                           attrs))]
        (remove nil? notifications)))
