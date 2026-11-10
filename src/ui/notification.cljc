(ns ui.notification
  "Notifications: a card for something that happened or is happening
   out of view (copied, a command running in the background, its
   result), and a stack that tucks older cards behind the newest one
   and fans them out on hover, focus or :expanded.

   Both are plain markup the host renders. The queue model at the end
   holds the rules (update by id, lifetimes by status, pause, mute,
   close all) as pure functions of the state and `now`; the host keeps
   the state wherever it keeps state and schedules `expire` at
   `next-due`. ui.toast is the runtime-owned one-line flash."
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

(defn- dismiss-all-el [on-dismiss-all dismiss-all-attrs]
  [:div {:class "notification-stack-bar"}
   [:button (merge #?(:squint (cond-> {:class "notification-stack-close" :type "button"}
                                on-dismiss-all (assoc :on-click on-dismiss-all))
                      :cljs   (cond-> {:class ["notification-stack-close"] :type "button"}
                                on-dismiss-all (assoc-in [:on :click] on-dismiss-all))
                      :clj    {:class "notification-stack-close" :type "button"})
                   dismiss-all-attrs)
    "Close all"]])

(defn notification-stack
  "A polite live region that stacks notifications, newest first: the first
   child sits in front, the next two tuck behind it as edges, the rest
   hide. Hover, keyboard focus or :expanded fans them out into a list.

   Props:
     :expanded          - boolean; fan out (set it on tap for touch screens)
     :position          - nil stays in the flow; :bottom-right, :bottom-center
                          or :bottom-left pins it to that viewport edge
     :label             - accessible name of the region (default \"Notifications\")
     :on-dismiss-all    - click handler of a Close all button shown above the
                          fanned-out list (:cljs, :squint)
     :dismiss-all-attrs - attributes for that button (HTMX on :clj)
     :class             - additional CSS classes
     :attrs             - additional HTML attributes map

   Children are notification elements, newest first."
  [{:keys [expanded position label on-dismiss-all dismiss-all-attrs class attrs] :as _props} & notifications]
  (let [notifications (remove nil? notifications)]
    (cond-> (into [:section (util/class-attrs (notification-stack-class-list {:expanded expanded :position position})
                                              class
                                              (merge {:aria-label (or label "Notifications")
                                                      :aria-live "polite"}
                                                     attrs))]
                  notifications)
      (and (or on-dismiss-all dismiss-all-attrs) (seq notifications))
      (conj (dismiss-all-el on-dismiss-all dismiss-all-attrs)))))

;; ── Queue model ───────────────────────────────────────────────────
;; {:items [item …] newest first, :muted #{source}, :paused-at ms, :seq n}
;; An item is the card's props plus :id, :source, :duration and the
;; :expires-at the model stamps on it. Time is always passed in as `now`.

(def default-limit 12)

(def default-lifetime 5000)

(def ^:private status-lifetimes
  {"loading" nil
   "danger"  nil
   "warning" 8000})

(defn queue
  "An empty notification queue."
  []
  {:items [] :muted #{} :paused-at nil :seq 0})

(defn lifetime
  "Milliseconds an item stays, nil until dismissed: its :duration (0 keeps
   it), else by :status: :loading and :danger stay, :warning 8 s, the
   rest 5 s."
  [{:keys [status duration]}]
  (if (number? duration)
    (when (pos? duration) duration)
    (let [s (some-> status util/kw-name)]
      (if (contains? status-lifetimes s)
        (get status-lifetimes s)
        default-lifetime))))

(defn- stamp
  "Sets :expires-at from the item's lifetime. While paused the clock
   starts at the pause, and resume shifts it by the pause's length."
  [item now paused-at]
  (let [ms (lifetime item)]
    (assoc item :expires-at (when ms (+ (or paused-at now) ms)))))

(defn post
  "Shows `item` (the card's props plus :id, :source and :duration). An
   :id already queued updates that card in place, its other keys kept,
   and restarts its lifetime; anything else goes in front, past `limit`
   (default 12) the oldest drop."
  ([state item now] (post state item now default-limit))
  ([state item now limit]
   (let [{:keys [items paused-at]} state
         id      (:id item)
         queued? (and (some? id) (some (fn [i] (= id (:id i))) items))]
     (if queued?
       (assoc state :items (mapv (fn [i] (if (= id (:id i)) (stamp (merge i item) now paused-at) i))
                                 items))
       (let [n    (inc (or (:seq state) 0))
             item (stamp (cond-> item (nil? id) (assoc :id (str "notification-" n))) now paused-at)]
         (assoc state :seq n :items (vec (take limit (cons item items)))))))))

(defn dismiss
  "Removes the item with `id`."
  [state id]
  (assoc state :items (vec (remove (fn [i] (= id (:id i))) (:items state)))))

(defn dismiss-all
  "Removes every item. A later post with the id of a dismissed running
   job shows its result as a new card."
  [state]
  (assoc state :items []))

(defn mute
  "Hides the items of `source` (kept and timed, but not visible) until
   unmute."
  [state source]
  (assoc state :muted (conj (or (:muted state) #{}) source)))

(defn unmute
  [state source]
  (assoc state :muted (disj (or (:muted state) #{}) source)))

(defn muted?
  [state source]
  (contains? (or (:muted state) #{}) source))

(defn pause
  "Stops every clock, while the stack is hovered or focused."
  [state now]
  (if (:paused-at state) state (assoc state :paused-at now)))

(defn resume
  "Restarts the clocks with the time each had left."
  [state now]
  (if-let [paused-at (:paused-at state)]
    (let [d (- now paused-at)]
      (assoc state
             :paused-at nil
             :items (mapv (fn [i] (cond-> i (:expires-at i) (assoc :expires-at (+ (:expires-at i) d))))
                          (:items state))))
    state))

(defn expire
  "Removes the items whose time is up; nothing while paused."
  [state now]
  (if (:paused-at state)
    state
    (assoc state :items (vec (remove (fn [i] (when-let [t (:expires-at i)] (<= t now)))
                                     (:items state))))))

(defn next-due
  "When to call expire next (ms, same clock as `now`), nil for never:
   the host schedules its timer from this."
  [state]
  (when-not (:paused-at state)
    (let [ts (keep (fn [i] (:expires-at i)) (:items state))]
      (when (seq ts) (apply min ts)))))

(defn visible
  "The items to render, newest first, muted sources left out."
  [state]
  (let [muted (or (:muted state) #{})]
    (vec (remove (fn [i] (contains? muted (:source i))) (:items state)))))
