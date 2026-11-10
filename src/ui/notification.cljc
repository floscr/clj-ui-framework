(ns ui.notification
  "Notifications after Sonner (sonner.emilkowal.ski), in this framework's card:
   a toaster that stacks toasts per screen position, the newest in front and
   the older ones tucked behind, fanned out on hover, swiped away by touch.

   Three layers. The queue model (end of this file) holds Sonner's rules as
   pure functions of the state and `now`; the host keeps that state and
   schedules one timer at `next-due`. `notification-toaster` renders the
   model's items. The ui.js.notification runtime measures heights, plays the
   enter, hover and swipe motion, and reports close, action, cancel and swipe
   as one `notificationdismiss` DOM event and hover, pointer and tab
   visibility as `notificationpause`. ui.toast is the runtime-owned one-line
   flash; `window.__uiNotify` (ui.js.notification) is Sonner's toast() API
   for pages without app state."
  (:require [clojure.string :as str]
            [ui.util :as util]
            [ui.icon :as icon]
            [ui.button :as button]))

;; ── Sonner's constants ─────────────────────────────────────────────

(def default-duration 4000)
(def remove-delay 200)
(def default-visible-toasts 3)
(def default-gap 14)
(def default-limit 50)
(def default-position :bottom-right)
(def default-hotkey ["altKey" "KeyT"])

(def ^:private infinity
  #?(:squint js/Infinity :cljs js/Infinity :clj Double/POSITIVE_INFINITY))

(defn- kw [x] (some-> x util/kw-name))

(defn- flag [x] (if x "true" "false"))

(defn- ->kw
  "A keyword from a string; squint's keywords are the strings themselves."
  [s]
  #?(:squint s :cljs (keyword s) :clj (keyword s)))

(defn- classes
  "Class tokens in the target's :class shape."
  [& tokens]
  (let [ts (remove nil? tokens)]
    #?(:squint (str/join " " ts)
       :cljs   (vec ts)
       :clj    (str/join " " ts))))

;; ── Card ───────────────────────────────────────────────────────────

(def ^:private type-icons
  {"success" :check
   "info"    :info
   "warning" :alert-triangle
   "error"   :x})

(defn notification-class-list
  "Classes for a card: its :type (:default when nil), :multiline when a
   description or meta follows the title."
  [{:keys [type multiline]}]
  (cond-> ["notification" (str "notification-" (or (kw type) "default"))]
    multiline (conj "notification-multiline")))

(defn notification-classes
  "Space-joined form of notification-class-list."
  [opts]
  (str/join " " (notification-class-list opts)))

(defn- loader
  "Sonner's twelve-bar loader. A toast that was loading keeps it to fade out
   while its result icon fades in."
  [visible?]
  (into [:span {:class "notification-loader" :data-visible (flag visible?) :aria-hidden "true"}]
        (repeat 12 [:span {:class "notification-loader-bar"}])))

(defn- glyph [icon-or-hiccup]
  (if (vector? icon-or-hiccup)
    icon-or-hiccup
    (icon/icon {:icon-name icon-or-hiccup :size :sm})))

(defn- icon-el [{:keys [type icon promise]} icons class-name]
  (when-not (false? icon)
    (let [t        (or (kw type) "default")
          loading? (= t "loading")
          custom   (get icons (->kw t))
          shown    (cond (some? icon)          (glyph icon)
                         (some? custom)        (glyph custom)
                         (get type-icons t)    (glyph (get type-icons t)))]
      (when (or loading? promise shown)
        (cond-> [:span {:class (classes "notification-icon" class-name) :data-icon ""}]
          (or loading? promise)    (conj (loader loading?))
          (and shown (not loading?)) (conj [:span {:class "notification-glyph"} shown]))))))

(defn- button-el
  "An action or cancel: {:label :on-click :attrs} or hiccup as is. The
   runtime dismisses the toast after the click unless it was prevented."
  [kind spec class-name]
  (if (vector? spec)
    spec
    (let [{:keys [label on-click attrs]} spec]
      (button/button (cond-> {:size :sm
                              :variant (if (= kind "action") :secondary :ghost)
                              :class class-name
                              :attrs (merge {:data-button "" (->kw (str "data-" kind)) ""} attrs)}
                       on-click (assoc :on-click on-click))
                     label))))

(defn- close-el [label disabled? class-name]
  [:button {:class (classes "notification-close" class-name) :type "button"
            :aria-label (or label "Close toast") :data-close-button ""
            :data-disabled (flag disabled?)}
   (icon/icon {:icon-name :x :size :sm})])

(defn- card-children
  [{:keys [type title description meta action cancel close-button close-label dismissible] :as item}
   icons class-names extra]
  (let [cn       (or class-names {})
        loading? (= "loading" (kw type))]
    (cond-> []
      (and close-button (not loading?))
      (conj (close-el close-label (false? dismissible) (:close-button cn)))
      true    (conj (icon-el item icons (:icon cn)))
      true    (conj (cond-> [:div {:class (classes "notification-text" (:content cn)) :data-content ""}
                             [:p {:class (classes "notification-title" (:title cn)) :data-title ""} title]]
                      description (conj [:div {:class (classes "notification-description" (:description cn))
                                               :data-description ""}
                                         description])
                      meta        (conj [:div {:class "notification-meta"} meta])))
      (or cancel action (seq extra))
      (conj (cond-> [:div {:class "notification-actions"}]
              cancel      (conj (button-el "cancel" cancel (:cancel-button cn)))
              action      (conj (button-el "action" action (:action-button cn)))
              (seq extra) (into extra))))))

(defn notification
  "One card, as a static block (previews, a page's own list). Inside a
   toaster use notification-toaster, which adds the stacking.

   Props:
     :type         - :default (nil), :success, :info, :warning, :error, :loading
     :title        - what happened, one line
     :description  - optional second line: the command, the file
     :meta         - optional muted line: elapsed time, counts
     :icon         - icon keyword or hiccup for the tile; false for none
     :action       - {:label :on-click :attrs} or hiccup; dismisses after the click
     :cancel       - same, a quieter button before the action
     :close-button - show the round close button on the top-left corner
     :close-label  - its aria-label (default \"Close toast\")
     :dismissible  - false: close and swipe do nothing
     :rich-colors  - tint the whole card with its type's color
     :invert       - the opposite theme's surface
     :unstyled     - no card surface, the host styles it
     :class-names  - extra classes: :title :description :content :icon
                     :close-button :action-button :cancel-button
     :class        - additional CSS classes
     :attrs        - additional HTML attributes map

   Children are extra actions after :cancel and :action."
  [{:keys [type title description meta icon action cancel close-button close-label dismissible
           rich-colors invert unstyled class-names class attrs] :as item} & children]
  (into [:div (util/class-attrs (notification-class-list {:type type :multiline (boolean (or description meta))})
                                class
                                (merge {:data-type (or (kw type) "default")
                                        :data-styled (flag (not unstyled))
                                        :data-rich-colors (flag rich-colors)
                                        :data-invert (flag invert)}
                                       attrs))]
        (card-children item nil class-names (remove nil? children))))

;; ── Toaster ────────────────────────────────────────────────────────

(defn position-parts
  "[y x] of a position keyword: :bottom-right → [\"bottom\" \"right\"]."
  [position]
  (str/split (or (kw position) (kw default-position)) #"-"))

(defn default-swipe-directions
  "Sonner's: toward the edge the toaster sits on, and toward its side."
  [position]
  (let [[y x] (position-parts position)]
    (cond-> [y] (not= x "center") (conj x))))

(defn hotkey-label
  "[\"altKey\" \"KeyT\"] → \"alt+T\"."
  [hotkey]
  (str/join "+" (map (fn [k]
                       (cond (str/starts-with? k "Key")   (subs k 3)
                             (str/starts-with? k "Digit") (subs k 5)
                             (str/ends-with? k "Key")     (subs k 0 (- (count k) 3))
                             :else k))
                     hotkey)))

(defn- css-length [v]
  (if (number? v) (str v "px") (str v)))

(defn offset-attrs
  "data-<prefix>-top/right/bottom/left from an offset: a number (px), a CSS
   length, or a map of sides. The runtime copies them to CSS variables."
  [prefix offset]
  (cond
    (nil? offset) {}
    (map? offset) (into {} (keep (fn [side]
                                   (when-let [v (get offset (->kw side))]
                                     [(->kw (str "data-" prefix "-" side)) (css-length v)])))
                        ["top" "right" "bottom" "left"])
    :else (into {} (map (fn [side] [(->kw (str "data-" prefix "-" side)) (css-length offset)]))
                ["top" "right" "bottom" "left"])))

(defn- toast-el
  "A toaster's <li>: the card plus the stacking state Sonner keeps per toast.
   The runtime adds data-mounted, data-expanded, data-swiping and the CSS
   variables; this markup never renders those, so a re-render keeps them."
  [{:keys [id type description meta rich-colors invert unstyled dismissible promise removed-at
           class-names class attrs] :as item}
   {:keys [index total visible-toasts y x close-button rich-colors-default invert-default icons]}]
  (let [t    (or (kw type) "default")
        item (cond-> item (nil? (:close-button item)) (assoc :close-button close-button))]
    (into [:li (util/class-attrs (notification-class-list {:type type :multiline (boolean (or description meta))})
                                 class
                                 (merge {:data-ui-notification ""
                                         :data-id (str id)
                                         :tabindex "0"
                                         :data-type t
                                         :data-styled (flag (not unstyled))
                                         :data-rich-colors (flag (if (some? rich-colors) rich-colors rich-colors-default))
                                         :data-invert (flag (or invert invert-default))
                                         :data-promise (flag promise)
                                         :data-removed (flag removed-at)
                                         :data-dismissible (flag (not (false? dismissible)))
                                         :data-index (str index)
                                         :data-count (str total)
                                         :data-front (flag (zero? index))
                                         :data-visible (flag (< index visible-toasts))
                                         :data-y-position y
                                         :data-x-position x}
                                        #?(:squint {} :cljs {:replicant/key (str id)} :clj {})
                                        attrs))]
          (card-children item icons class-names nil))))

(defn- event-attrs [on-dismiss on-pause]
  #?(:squint (cond-> {}
               on-dismiss (assoc :on-notificationdismiss
                                 (fn [e] (on-dismiss (.. e -detail -id) (.. e -detail -reason))))
               on-pause   (assoc :on-notificationpause
                                 (fn [e] (on-pause (.. e -detail -paused)))))
     :cljs   (cond-> {}
               on-dismiss (assoc-in [:on :notificationdismiss]
                                    (fn [e] (on-dismiss (.. e -detail -id) (keyword (.. e -detail -reason)))))
               on-pause   (assoc-in [:on :notificationpause]
                                    (fn [e] (on-pause (.. e -detail -paused)))))
     :clj    {}))

(defn notification-toaster
  "Sonner's Toaster: a polite live region with one list per position, each
   toast an <li>. Needs the ui.js.notification runtime, which shows the
   toasts (they enter from the edge once it marks them mounted).

   Props:
     :items             - the toasts, newest first: notification props plus
                          :id, :position, :removed-at (queue model's visible)
     :position          - :top-left :top-center :top-right :bottom-left
                          :bottom-center :bottom-right (default)
     :expand            - always fanned out
     :visible-toasts    - how many show in the stack (3)
     :gap               - px between expanded toasts (14)
     :offset            - distance from the viewport edge: px number, CSS
                          length or {:top :right :bottom :left} (24px)
     :mobile-offset     - the same under 600px wide (16px)
     :close-button      - close buttons on every toast
     :rich-colors       - rich colors on every toast
     :invert            - every toast on the opposite theme's surface
     :dir               - \"ltr\" or \"rtl\"
     :hotkey            - keys that focus and fan out the toaster
                          ([\"altKey\" \"KeyT\"])
     :label             - accessible name (\"Notifications\"), hotkey appended
     :swipe-directions  - [\"top\" \"right\" \"bottom\" \"left\"] subset; default
                          toward the toaster's edge and side
     :icons             - {:success :info :warning :error :loading} keyword or hiccup
     :contained         - positioned in the nearest positioned ancestor instead
                          of the viewport (not in Sonner)
     :on-dismiss        - (fn [id reason]) when a toast is closed, swiped or its
                          action or cancel clicked; reason :close :swipe :action
                          :cancel (:cljs, :squint)
     :on-pause          - (fn [paused?]) while hovered, pressed or the tab hidden
     :class             - additional CSS classes
     :attrs             - additional HTML attributes map (HTMX on :clj)"
  [{:keys [items position expand visible-toasts gap offset mobile-offset close-button rich-colors
           invert dir hotkey label swipe-directions icons contained on-dismiss on-pause class attrs]}]
  (let [pos       (or (kw position) (kw default-position))
        hotkey    (or hotkey default-hotkey)
        visible-n (or visible-toasts default-visible-toasts)
        positions (distinct (cons pos (keep (fn [i] (kw (:position i))) items)))]
    (into [:section (util/class-attrs (cond-> ["notification-toaster"]
                                        contained (conj "notification-toaster-contained"))
                                      class
                                      (merge {:data-ui-notification-toaster ""
                                              :tabindex "-1"
                                              :aria-label (str (or label "Notifications") " " (hotkey-label hotkey))
                                              :aria-live "polite"
                                              :aria-relevant "additions text"
                                              :aria-atomic "false"
                                              :data-hotkey (str/join "+" hotkey)}
                                             (event-attrs on-dismiss on-pause)
                                             attrs))]
          (when (seq items)
            (for [p positions
                  :let [[y x]  (position-parts p)
                        here   (filterv (fn [i] (= p (or (kw (:position i)) pos))) items)
                        n      (count here)
                        ctx    {:total n :visible-toasts visible-n :y y :x x
                                :close-button close-button :rich-colors-default rich-colors
                                :invert-default invert :icons icons}]]
              (into [:ol (merge {:class "notification-list"
                                 :data-ui-notification-list ""
                                 :data-y-position y
                                 :data-x-position x
                                 :tabindex "-1"
                                 :data-expand (flag expand)
                                 :data-gap (str (or gap default-gap))
                                 :data-swipe-directions (str/join " " (or swipe-directions
                                                                          (default-swipe-directions p)))}
                                (when dir {:dir dir})
                                (offset-attrs "offset" offset)
                                (offset-attrs "mobile-offset" mobile-offset)
                                #?(:squint {} :cljs {:replicant/key p} :clj {}))]
                    (map-indexed (fn [i item] (toast-el item (assoc ctx :index i))) here)))))))

;; ── Queue model ────────────────────────────────────────────────────
;; {:items [item …] newest first, :muted #{source}, :paused-at ms, :seq n}
;; An item is a toast's props plus :id, :source, :duration, and what the
;; model stamps: :expires-at, and while it leaves :removed-at and :closed
;; (:auto, :dismiss, or the runtime's reason). Time is passed in as `now`.

(defn queue
  "An empty notification queue."
  []
  {:items [] :muted #{} :paused-at nil :seq 0})

(defn lifetime
  "Milliseconds an item stays, nil until dismissed. Sonner's rule: :loading
   stays, a positive finite :duration counts (##Inf stays), anything else
   is the toaster's `default` (4 s)."
  ([item] (lifetime item default-duration))
  ([{:keys [type duration]} default]
   (cond
     (= "loading" (kw type))                            nil
     (and (number? duration) (= duration infinity))     nil
     (and (number? duration) (pos? duration))           duration
     :else                                              default)))

(defn- stamp [item now paused-at default]
  (let [ms (lifetime item default)]
    (-> item
        (dissoc :removed-at :closed)
        (assoc :expires-at (when ms (+ (or paused-at now) ms))))))

(defn- close [item now reason]
  (if (:removed-at item) item (assoc item :removed-at now :closed reason)))

(defn post
  "Shows `item`. A queued :id updates that toast in place (other keys kept,
   clock restarted; a loading one keeps :promise so its loader fades out);
   one that is leaving is replaced by `item`; anything else goes in front,
   the oldest dropping past :limit. Opts: :duration (the default lifetime),
   :limit."
  ([state item now] (post state item now nil))
  ([state item now {:keys [duration limit]}]
   (let [{:keys [items paused-at]} state
         dur   (or duration default-duration)
         id    (:id item)
         old   (when (some? id) (some (fn [i] (when (= id (:id i)) i)) items))
         fresh (fn [i] (stamp i now paused-at dur))]
     (cond
       (and old (:removed-at old))
       (assoc state :items (mapv (fn [i] (if (= id (:id i)) (fresh item) i)) items))

       old
       (assoc state :items (mapv (fn [i]
                                   (if (= id (:id i))
                                     (fresh (cond-> (merge i item)
                                              (= "loading" (kw (:type i))) (assoc :promise true)))
                                     i))
                                 items))

       :else
       (let [n    (inc (or (:seq state) 0))
             item (fresh (cond-> item (nil? id) (assoc :id (str "notification-" n))))]
         (assoc state :seq n :items (vec (take (or limit default-limit) (cons item items)))))))))

(defn dismiss
  "Starts closing `id`: it stays remove-delay ms with :removed-at for its
   exit, then expire drops it. `reason` is kept as :closed (:dismiss)."
  ([state id now] (dismiss state id now :dismiss))
  ([state id now reason]
   (assoc state :items (mapv (fn [i] (if (= id (:id i)) (close i now reason) i)) (:items state)))))

(defn dismiss-all
  "Starts closing every item. A later post with a closed job's id shows its
   result as a new toast."
  [state now]
  (assoc state :items (mapv (fn [i] (close i now :dismiss)) (:items state))))

(defn mute
  "Hides the items of `source` (kept and timed, but not visible) until unmute."
  [state source]
  (assoc state :muted (conj (or (:muted state) #{}) source)))

(defn unmute
  [state source]
  (assoc state :muted (disj (or (:muted state) #{}) source)))

(defn muted?
  [state source]
  (contains? (or (:muted state) #{}) source))

(defn pause
  "Stops every clock: Sonner pauses while the toaster is expanded, pressed
   or the tab hidden."
  [state now]
  (if (:paused-at state) state (assoc state :paused-at now)))

(defn resume
  "Restarts the clocks with the time each had left."
  [state now]
  (if-let [paused-at (:paused-at state)]
    (let [d (- now paused-at)]
      (assoc state
             :paused-at nil
             :items (mapv (fn [i] (cond-> i
                                    (and (:expires-at i) (not (:removed-at i)))
                                    (assoc :expires-at (+ (:expires-at i) d))))
                          (:items state))))
    state))

(defn expire
  "Starts closing the items whose time is up (:closed :auto; not while
   paused) and drops those that finished leaving."
  [state now]
  (let [paused? (:paused-at state)]
    (assoc state :items (into [] (keep (fn [i]
                                         (cond
                                           (:removed-at i)
                                           (when (< now (+ (:removed-at i) remove-delay)) i)

                                           (and (not paused?) (:expires-at i) (<= (:expires-at i) now))
                                           (close i now :auto)

                                           :else i)))
                              (:items state)))))

(defn next-due
  "When to call expire next (same clock as `now`), nil for never: the
   earliest clock (none while paused) or end of an exit."
  [state]
  (let [paused? (:paused-at state)
        ts      (keep (fn [i]
                        (if-let [r (:removed-at i)]
                          (+ r remove-delay)
                          (when-not paused? (:expires-at i))))
                      (:items state))]
    (when (seq ts) (apply min ts))))

(defn newly-closed
  "Items closing in `after` that were not in `before`: the host's cue for
   Sonner's onAutoClose (:closed :auto) and onDismiss (any other)."
  [before after]
  (let [was (set (keep (fn [i] (when (:removed-at i) (:id i))) (:items before)))]
    (filterv (fn [i] (and (:removed-at i) (not (contains? was (:id i))))) (:items after))))

(defn visible
  "The items to render, newest first, muted sources left out. Leaving items
   stay in until expire drops them, for their exit."
  [state]
  (let [muted (or (:muted state) #{})]
    (filterv (fn [i] (not (contains? muted (:source i)))) (:items state))))

(defn active
  "Visible items that are not leaving."
  [state]
  (filterv (fn [i] (not (:removed-at i))) (visible state)))
