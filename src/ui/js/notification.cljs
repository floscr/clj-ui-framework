(ns notification
  "Runtime for ui.notification's toaster: Sonner's behaviour on any markup
   with `[data-ui-notification-toaster]`, server-rendered or re-rendered by
   an SPA, found by a MutationObserver.

   Per list it measures every toast and sets the CSS variables Sonner keeps
   in React state (--index, --toasts-before, --z-index, --offset,
   --initial-height, --front-toast-height, --gap, the offsets), marks new
   toasts data-mounted a frame after they appear (their enter), and keeps
   data-expanded: hover, a pointer down or the hotkey fan the toaster out.
   It swipes toasts (direction lock, damping toward a disallowed side,
   dismiss at 45px or 0.11px/ms) and turns close, action and cancel clicks
   and swipes into one bubbling `notificationdismiss` event {id, reason} on
   the toast; `notificationpause` {paused} on the toaster while it is
   expanded, pressed or the tab hidden. The host owns the toasts (ui.
   notification's queue model) and renders data-removed.

   window.__uiNotify is Sonner's toast() for pages without app state: its
   own queue and toaster, rendered from the same markup."
  (:require [ui.notification :as n]))

(def ^:private swipe-threshold 45)
(def ^:private velocity-threshold 0.11)

;; Attributes this runtime owns: a host re-render never renders them.
(def ^:private own-attrs
  #{"data-mounted" "data-expanded" "data-swiping" "data-swiped" "data-swipe-out"
    "data-swipe-direction" "style"})

;; ── Pure parts ────────────────────────────────────────────────────

(defn offsets
  "Each live toast's offset from the edge: the gaps and heights before it."
  [heights gap]
  (loop [hs heights i 0 sum 0 out []]
    (if (empty? hs)
      out
      (recur (rest hs) (inc i) (+ sum (first hs)) (conj out (+ (* i gap) sum))))))

(defn dampen
  "Sonner's resistance toward a side the toast can't be swiped to."
  [delta]
  (let [d (* delta (/ 1 (+ 1.5 (/ (js/Math.abs delta) 20))))]
    (if (< (js/Math.abs d) (js/Math.abs delta)) d delta)))

(defn swipe-amount
  "{x y} the toast follows for a drag of dx, dy locked to `axis`."
  [axis dx dy directions]
  (let [has? (fn [d] (some (fn [x] (= x d)) directions))]
    (cond
      (= axis "y") {:x 0 :y (cond (not (or (has? "top") (has? "bottom"))) 0
                                  (or (and (has? "top") (< dy 0)) (and (has? "bottom") (> dy 0))) dy
                                  :else (dampen dy))}
      (= axis "x") {:y 0 :x (cond (not (or (has? "left") (has? "right"))) 0
                                  (or (and (has? "left") (< dx 0)) (and (has? "right") (> dx 0))) dx
                                  :else (dampen dx))}
      :else {:x 0 :y 0})))

(defn swipe-out
  "The direction a released drag dismisses toward, or nil: in an allowed
   direction, past the threshold or faster than the velocity threshold."
  [axis x y ms directions]
  (let [amount  (if (= axis "x") x y)
        dir     (if (= axis "x") (if (> x 0) "right" "left") (if (> y 0) "bottom" "top"))
        allowed (some (fn [d] (= d dir)) directions)
        v       (/ (js/Math.abs amount) (max ms 1))]
    (when (and axis allowed (or (>= (js/Math.abs amount) swipe-threshold) (> v velocity-threshold)))
      (get {"right" "right" "left" "left" "bottom" "down" "top" "up"} dir))))

;; ── Helpers ───────────────────────────────────────────────────────

(defn- attr [el k] (.getAttribute el k))
(defn- set-attr! [el k v] (when (not= (attr el k) v) (.setAttribute el k v)))
(defn- set-var! [el k v] (.setProperty (.-style el) k v))
(defn- toasts [ol] (js/Array.from (.querySelectorAll ol ":scope > [data-ui-notification]")))
(defn- lists [toaster] (js/Array.from (.querySelectorAll toaster "[data-ui-notification-list]")))
(defn- live? [li] (not= "true" (attr li "data-removed")))
(defn- toaster-of [el] (.closest el "[data-ui-notification-toaster]"))

(defn- emit! [el type detail]
  (.dispatchEvent el (js/CustomEvent. type #js {:bubbles true :detail (clj->js detail)})))

(defn- natural-height
  "The toast's own height, whatever the collapsed stack makes of it: its
   layout height, since a tucked toast's bounding box is scaled down."
  [li]
  (let [style (.-style li)
        h0    (.-height style)]
    (set! (.-height style) "auto")
    (let [h (.-offsetHeight li)]
      (set! (.-height style) h0)
      h)))

;; ── Expanded, pressed, paused ─────────────────────────────────────

(defn- state [toaster]
  (or (aget toaster "__uiN")
      (let [s #js {:expanded false :interacting false :paused false :lastFocus nil :focusWithin false}]
        (aset toaster "__uiN" s)
        s)))

(declare sync-toaster!)

(defn- update-pause! [toaster]
  (let [s      (state toaster)
        paused (boolean (or (aget s "expanded") (aget s "interacting") (.-hidden js/document)))]
    (when (not= paused (aget s "paused"))
      (aset s "paused" paused)
      (emit! toaster "notificationpause" {:paused paused}))))

(defn- set-expanded! [toaster expanded?]
  (let [s (state toaster)]
    (when (not= expanded? (aget s "expanded"))
      (aset s "expanded" expanded?)
      (sync-toaster! toaster)
      (update-pause! toaster))))

;; ── Sync: variables and flags Sonner keeps per toast ──────────────

(defn- copy-offsets! [ol]
  (doseq [prefix ["offset" "mobile-offset"]
          side ["top" "right" "bottom" "left"]]
    (when-let [v (attr ol (str "data-" prefix "-" side))]
      (set-var! ol (str "--" prefix "-" side) v)))
  (when-let [g (attr ol "data-gap")]
    (set-var! ol "--gap" (str g "px"))))

(defn- sync-list! [toaster ol]
  (copy-offsets! ol)
  (let [items     (toasts ol)
        gap       (or (js/parseFloat (attr ol "data-gap")) n/default-gap)
        live      (filterv live? items)
        heights   (mapv natural-height live)
        offs      (offsets heights gap)
        by-li     (js/Map.)
        total     (count items)
        expand?   (= "true" (attr ol "data-expand"))
        expanded? (and (aget (state toaster) "expanded") (> (count live) 1))]
    (doseq [[i li] (map-indexed vector live)]
      (.set by-li li #js [(nth heights i) (nth offs i)]))
    (set-var! ol "--front-toast-height" (str (or (first heights) 0) "px"))
    (doseq [[idx li] (map-indexed vector items)]
      (let [mounted? (= "true" (attr li "data-mounted"))]
        (set-var! li "--index" (str idx))
        (set-var! li "--toasts-before" (str idx))
        (set-var! li "--z-index" (str (- total idx)))
        (when-let [hv (.get by-li li)]
          (set-var! li "--offset" (str (aget hv 1) "px"))
          (set-var! li "--initial-height" (if expand? "auto" (str (aget hv 0) "px"))))
        (doseq [k ["data-swiping" "data-swiped" "data-swipe-out"]]
          (when-not (attr li k) (.setAttribute li k "false")))
        (set-attr! li "data-expanded" (if (or expanded? (and expand? mounted?)) "true" "false"))
        (when-not mounted?
          (when-not (aget li "__uiNMounting")
            (aset li "__uiNMounting" true)
            (js/requestAnimationFrame
             (fn [] (.setAttribute li "data-mounted" "true")
               (set-attr! li "data-expanded" (if (or (aget (state toaster) "expanded") expand?) "true" "false"))))))))))

;; ── Swipe (per toast) ─────────────────────────────────────────────

(defn- swipe-directions [li]
  (let [ol (.closest li "[data-ui-notification-list]")]
    (.split (or (some-> ol (attr "data-swipe-directions")) "") " ")))

(defn- swipable? [li]
  (and (= "true" (attr li "data-dismissible")) (not= "loading" (attr li "data-type"))))

(defn- bind-toast! [li]
  (when-not (aget li "__uiNBound")
    (aset li "__uiNBound" true)
    (let [drag #js {:start nil :t 0 :axis nil}]
      (.addEventListener li "pointerdown"
        (fn [e]
          (when (and (not= 2 (.-button e)) (swipable? li))
            (aset drag "t" (js/Date.now))
            ;; keeps the drag when the pointer leaves the toast; a pointer the
            ;; browser no longer tracks only loses that
            (try (.setPointerCapture (.-target e) (.-pointerId e)) (catch :default _ nil))
            (when-not (= "BUTTON" (.-tagName (.-target e)))
              (.setAttribute li "data-swiping" "true")
              (aset drag "start" #js {:x (.-clientX e) :y (.-clientY e)})))))
      (.addEventListener li "pointermove"
        (fn [e]
          (when-let [start (aget drag "start")]
            (when-not (pos? (.-length (str (.getSelection js/window))))
              (let [dx (- (.-clientX e) (aget start "x"))
                    dy (- (.-clientY e) (aget start "y"))]
                (when (and (nil? (aget drag "axis")) (or (> (js/Math.abs dx) 1) (> (js/Math.abs dy) 1)))
                  (aset drag "axis" (if (> (js/Math.abs dx) (js/Math.abs dy)) "x" "y")))
                (let [{:keys [x y]} (swipe-amount (aget drag "axis") dx dy (swipe-directions li))]
                  (when (or (pos? (js/Math.abs x)) (pos? (js/Math.abs y)))
                    (.setAttribute li "data-swiped" "true"))
                  (set-var! li "--swipe-amount-x" (str x "px"))
                  (set-var! li "--swipe-amount-y" (str y "px"))))))))
      (.addEventListener li "pointerup"
        (fn [_]
          (when (and (swipable? li) (not= "true" (attr li "data-swipe-out")))
            (let [x   (or (js/parseFloat (.getPropertyValue (.-style li) "--swipe-amount-x")) 0)
                  y   (or (js/parseFloat (.getPropertyValue (.-style li) "--swipe-amount-y")) 0)
                  out (swipe-out (aget drag "axis") x y (- (js/Date.now) (aget drag "t")) (swipe-directions li))]
              (aset drag "start" nil)
              (if out
                (do (.setAttribute li "data-swipe-direction" out)
                    (.setAttribute li "data-swipe-out" "true")
                    (emit! li "notificationdismiss" {:id (attr li "data-id") :reason "swipe"}))
                (do (set-var! li "--swipe-amount-x" "0px")
                    (set-var! li "--swipe-amount-y" "0px")
                    (.setAttribute li "data-swiped" "false")
                    (.setAttribute li "data-swiping" "false")
                    (aset drag "axis" nil)))))))
      (.addEventListener li "dragend"
        (fn [_]
          (.setAttribute li "data-swiping" "false")
          (aset drag "start" nil)
          (aset drag "axis" nil))))))

;; ── Lists: hover, pointer, focus ──────────────────────────────────

(defn- not-dismissible-target? [e]
  (some-> (.-target e) (.closest "[data-dismissible='false']")))

(defn- bind-list! [toaster ol]
  (when-not (aget ol "__uiNBound")
    (aset ol "__uiNBound" true)
    (let [s (state toaster)]
      (.addEventListener ol "mouseenter" (fn [_] (set-expanded! toaster true)))
      (.addEventListener ol "mousemove" (fn [_] (set-expanded! toaster true)))
      (.addEventListener ol "mouseleave" (fn [_] (when-not (aget s "interacting") (set-expanded! toaster false))))
      (.addEventListener ol "dragend" (fn [_] (set-expanded! toaster false)))
      (.addEventListener ol "pointerdown"
        (fn [e] (when-not (not-dismissible-target? e)
                  (aset s "interacting" true)
                  (update-pause! toaster))))
      (.addEventListener ol "pointerup"
        (fn [_] (aset s "interacting" false) (update-pause! toaster)))
      (.addEventListener ol "focusin"
        (fn [e]
          (when-not (or (not-dismissible-target? e) (aget s "focusWithin"))
            (aset s "focusWithin" true)
            (aset s "lastFocus" (.-relatedTarget e)))))
      (.addEventListener ol "focusout"
        (fn [e]
          (when (and (aget s "focusWithin") (not (.contains ol (.-relatedTarget e))))
            (aset s "focusWithin" false)
            (when-let [prev (aget s "lastFocus")]
              (.focus prev #js {:preventScroll true})
              (aset s "lastFocus" nil))))))))

(defn- sync-toaster! [toaster]
  (doseq [ol (lists toaster)]
    (bind-list! toaster ol)
    (doseq [li (toasts ol)] (bind-toast! li))
    (sync-list! toaster ol))
  (when (<= (reduce + 0 (map (fn [ol] (count (filter live? (toasts ol)))) (lists toaster))) 1)
    (when (aget (state toaster) "expanded")
      (aset (state toaster) "expanded" false)
      (update-pause! toaster))))

(defn- sync-all! []
  (doseq [t (js/Array.from (.querySelectorAll js/document "[data-ui-notification-toaster]"))]
    (sync-toaster! t)))

(def ^:private scheduled (atom false))

(defn- schedule! []
  (when-not @scheduled
    (reset! scheduled true)
    (js/requestAnimationFrame (fn [] (reset! scheduled false) (sync-all!)))))

;; ── Document listeners: clicks, keys, visibility ──────────────────

(defn- on-click [e]
  (when-not (.-defaultPrevented e)
    (when-let [li (some-> (.-target e) (.closest "[data-ui-notification]"))]
      (let [dismissible? (= "true" (attr li "data-dismissible"))
            target       (.-target e)
            reason       (cond (.closest target "[data-close-button]") (when dismissible? "close")
                               (.closest target "[data-action]")       "action"
                               (.closest target "[data-cancel]")       (when dismissible? "cancel"))]
        (when reason
          (emit! li "notificationdismiss" {:id (attr li "data-id") :reason reason}))))))

(defn- hotkey-pressed? [e keys]
  (and (pos? (count keys))
       (every? (fn [k] (or (aget e k) (= (.-code e) k))) keys)))

(defn- on-keydown [e]
  (doseq [t (js/Array.from (.querySelectorAll js/document "[data-ui-notification-toaster]"))]
    (let [keys (.split (or (attr t "data-hotkey") "") "+")]
      (when (hotkey-pressed? e keys)
        (set-expanded! t true)
        (some-> (first (lists t)) (.focus)))
      (when (and (= "Escape" (.-code e)) (.contains t (.-activeElement js/document)))
        (set-expanded! t false)))))

;; ── window.__uiNotify: Sonner's toast() on its own queue ──────────

(def ^:private svg-ns "http://www.w3.org/2000/svg")

(defn- element? [h] (and (vector? h) (string? (first h))))

(defn- plain-map? [x] (and (some? x) (not (vector? x)) (= (.-constructor x) js/Object)))

(defn- set-attrs! [el attrs]
  (doseq [[k v] (js/Object.entries (or attrs #js {}))]
    (cond
      (and (.startsWith k "on-") (fn? v)) (.addEventListener el (.slice k 3) v)
      (= k "style")      (doseq [[sk sv] (js/Object.entries v)] (.setProperty (.-style el) sk (str sv)))
      (or (nil? v) (false? v)) nil
      (true? v)          (.setAttribute el k "")
      :else              (.setAttribute el k (str v)))))

(defn- dom
  "A DOM node from squint hiccup ([tag attrs? & children], strings, seqs)."
  ([h] (dom h false))
  ([h svg?]
   (cond
     (nil? h) nil
     (or (string? h) (number? h)) (js/document.createTextNode (str h))
     (element? h)
     (let [tag   (first h)
           svg?  (or svg? (= tag "svg"))
           el    (if svg? (js/document.createElementNS svg-ns tag) (js/document.createElement tag))
           [attrs kids] (if (plain-map? (second h)) [(second h) (drop 2 h)] [nil (rest h)])]
       (set-attrs! el attrs)
       (doseq [c (mapcat (fn [c] (if (or (nil? c) (string? c) (number? c) (element? c)) [c] c)) kids)]
         (when-let [node (dom c svg?)] (.appendChild el node)))
       el)
     :else nil)))

(defn- patch-attrs! [el attrs]
  (let [keep (js/Set.)]
    (doseq [[k v] (js/Object.entries (or attrs #js {}))]
      (when (and (some? v) (not (false? v)) (not (and (.startsWith k "on-") (fn? v))))
        (.add keep k)))
    (doseq [a (js/Array.from (.-attributes el))]
      (let [k (.-name a)]
        (when-not (or (.has keep k) (contains? own-attrs k)) (.removeAttribute el k)))))
  (doseq [[k v] (js/Object.entries (or attrs #js {}))]
    (cond (or (nil? v) (false? v) (.startsWith k "on-")) nil
          (true? v) (set-attr! el k "")
          :else (set-attr! el k (str v)))))

(defn- patch-list!
  "Keyed update of `ol` from the list's hiccup: a toast keeps its element (and
   with it its motion), only its attributes and content change."
  [ol h]
  (patch-attrs! ol (second h))
  (let [items  (vec (drop 2 h))
        wanted (js/Set. (map (fn [i] (aget (second i) "data-id")) items))]
    (doseq [li (toasts ol)]
      (when-not (.has wanted (attr li "data-id")) (.remove li)))
    (doseq [[idx item] (map-indexed vector items)]
      (let [id       (aget (second item) "data-id")
            existing (.querySelector ol (str ":scope > [data-id=\"" (js/CSS.escape id) "\"]"))
            fresh    (dom item)]
        (if existing
          (do (patch-attrs! existing (second item))
              (.apply (.-replaceChildren existing) existing (js/Array.from (.-childNodes fresh))))
          (.insertBefore ol fresh (aget (toasts ol) idx)))))))

(defn- patch-toaster! [root h]
  (patch-attrs! root (second h))
  (let [ols (vec (drop 2 h))
        key (fn [a] (str (aget a "data-y-position") "-" (aget a "data-x-position")))
        wanted (js/Set. (map (fn [o] (key (second o))) ols))]
    (doseq [ol (lists root)]
      (when-not (.has wanted (str (attr ol "data-y-position") "-" (attr ol "data-x-position")))
        (.remove ol)))
    (doseq [o ols]
      (let [k  (key (second o))
            ol (or (.querySelector root (str "[data-ui-notification-list][data-y-position=\""
                                             (aget (second o) "data-y-position")
                                             "\"][data-x-position=\""
                                             (aget (second o) "data-x-position") "\"]"))
                   (let [el (dom [(first o) (second o)])] (.appendChild root el) el))]
        (patch-list! ol o)
        k))))

(defn- kebab [s]
  (.replace s (js/RegExp. "[A-Z]" "g") (fn [c] (str "-" (.toLowerCase c)))))

(defn- props
  "A JS options object as ui.notification props: camelCase keys kebab-cased,
   actions too."
  [o]
  (let [m (js/Object.fromEntries
           (map (fn [[k v]] [(kebab k) v]) (js/Object.entries (or o #js {}))))]
    (doseq [k ["action" "cancel"]]
      (when (plain-map? (aget m k)) (aset m k (props (aget m k)))))
    m))

(def ^:private api
  #js {:queue (n/queue) :options #js {} :root nil :timer nil})

(declare render!)

(defn- now [] (js/Date.now))

(defn- fire-closed! [before after]
  (doseq [i (n/newly-closed before after)]
    (if (= "auto" (:closed i))
      (when-let [f (:on-auto-close i)] (f (clj->js i)))
      (when-let [f (:on-dismiss i)] (f (clj->js i))))))

(defn- update-queue! [f & args]
  (let [before (aget api "queue")
        after  (apply f before args)]
    (aset api "queue" after)
    (fire-closed! before after)
    (render!)))

(defn- schedule-expire! []
  (when-let [t (aget api "timer")] (js/clearTimeout t))
  (aset api "timer"
        (when-let [due (n/next-due (aget api "queue"))]
          (js/setTimeout (fn [] (update-queue! n/expire (now))) (max 0 (- due (now)))))))

(defn- ensure-root! []
  (or (aget api "root")
      (let [el (dom (n/notification-toaster (props (aget api "options"))))]
        (.addEventListener el "notificationdismiss"
          (fn [e] (update-queue! n/dismiss (.. e -detail -id) (now) (.. e -detail -reason))))
        (.addEventListener el "notificationpause"
          (fn [e] (update-queue! (if (.. e -detail -paused) n/pause n/resume) (now))))
        (.appendChild js/document.body el)
        (aset api "root" el)
        el)))

(defn- render! []
  (let [root (ensure-root!)
        h    (n/notification-toaster (assoc (props (aget api "options"))
                                            :items (n/visible (aget api "queue"))))]
    (patch-toaster! root h)
    (schedule-expire!)
    (schedule!)))

(defn- create!
  [message type opts]
  ;; :type nil resets it, as Sonner's toast() / toast.message do
  (let [item (assoc (props opts) :title message :type type)]
    (update-queue! n/post item (now) #js {:duration (aget (aget api "options") "duration")})
    (or (:id item) (str "notification-" (:seq (aget api "queue"))))))

(defn- notify [message opts] (create! message nil opts))

(defn- promise! [p data]
  (let [d  (props data)
        id (when (some? (:loading d))
             (create! (:loading d) "loading" (assoc (or data #js {}) :id (or (:id d) (str "promise-" (now))))))
        settle (fn [kind v]
                 (let [spec (get d kind)
                       spec (if (fn? spec) (spec v) spec)
                       base (if (plain-map? spec) spec #js {:message spec})
                       desc (:description d)]
                   (when (some? spec)
                     (create! (or (aget base "message") (:title base))
                              (if (= kind "success") "success" "error")
                              (cond-> (assoc base :id id)
                                (some? desc) (assoc :description (if (fn? desc) (desc v) desc)))))))]
    (-> (js/Promise.resolve (if (fn? p) (p) p))
        (.then (fn [v] (if (some? (:success d)) (settle "success" v) (when id (update-queue! n/dismiss id (now))))))
        (.catch (fn [err] (if (some? (:error d)) (settle "error" err) (when id (update-queue! n/dismiss id (now))))))
        (.finally (fn [] (when-let [f (:finally d)] (f)))))
    id))

(defn- consume-markers! []
  (doseq [el (js/Array.from (.querySelectorAll js/document "[data-ui-notify]"))]
    (let [ds (.-dataset el)]
      (.remove el)
      (create! (aget ds "uiNotify") (aget ds "type")
               #js {:id (aget ds "id") :description (aget ds "description")
                    :duration (some-> (aget ds "duration") js/parseFloat)}))))

(def notify-api
  (js/Object.assign
   notify
   #js {:success (fn [m o] (create! m "success" o))
        :error   (fn [m o] (create! m "error" o))
        :info    (fn [m o] (create! m "info" o))
        :warning (fn [m o] (create! m "warning" o))
        :loading (fn [m o] (create! m "loading" o))
        :message (fn [m o] (create! m nil o))
        :promise promise!
        :dismiss (fn [id] (if (some? id) (update-queue! n/dismiss id (now)) (update-queue! n/dismiss-all (now))))
        :getToasts (fn [] (clj->js (n/active (aget api "queue"))))
        :toaster (fn [o] (aset api "options" o) (when (aget api "root") (render!)))}))

;; ── Start ─────────────────────────────────────────────────────────

(def ^:private watched "[data-ui-notification-toaster],[data-ui-notify]")

(defn- relevant?
  "A mutation inside a toaster, or one adding a toaster or a marker: the page's
   other changes (a streaming chat) cost one check each."
  [r]
  (let [t  (.-target r)
        el (if (= 1 (.-nodeType t)) t (.-parentElement t))]
    (or (some-> el (.closest "[data-ui-notification-toaster]"))
        (some (fn [node]
                (and (= 1 (.-nodeType node))
                     (or (.matches node watched) (.querySelector node watched))))
              (js/Array.from (.-addedNodes r))))))

(defn init! []
  (sync-all!)
  (consume-markers!)
  (.addEventListener js/document "click" on-click)
  (.addEventListener js/document "keydown" on-keydown)
  (.addEventListener js/document "visibilitychange"
    (fn [] (doseq [t (js/Array.from (.querySelectorAll js/document "[data-ui-notification-toaster]"))]
             (update-pause! t))))
  (.addEventListener js/window "resize" schedule!)
  (let [obs (js/MutationObserver.
             (fn [records]
               (when (some relevant? records)
                 (consume-markers!)
                 (schedule!))))]
    (.observe obs js/document.documentElement
              #js {:childList true :subtree true :characterData true
                   :attributes true :attributeFilter #js ["data-removed" "data-index" "data-type"]})))

(aset js/window "__uiNotify" notify-api)

(if (= "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" init!)
  (init!))
