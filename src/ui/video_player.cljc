(ns ui.video-player
  "Video player — glass capsules over the picture, a popover per control.

   The markup is rendered here; the `ui.js.video-player` runtime (in
   ui-runtime.js) finds every [data-video-player] element and owns playback:
   hls.js when the page loaded it (window.Hls) and the source is a .m3u8,
   scrubbing, volume, captions drawn from the video's text tracks, idle
   hiding, fullscreen, the up-next countdown and the popovers.

   Controls are data. Built-ins: :subtitles :quality :speed (menus the
   runtime fills from the stream), :autonext (toggle, needs :next), :pip and
   :fullscreen. Custom ones come from :buttons:

     {:id :sleep :icon :moon :label \"Sleep timer\"
      :options [{:value 0 :label \"Off\"} {:value 30 :label \"30 minutes\"}]
      :value 0}                                   ; a menu (default type)
     {:id :loop :type :toggle :icon :repeat :label \"Loop\" :pressed? false}
     {:id :share :type :action :icon :share :label \"Share\"}

   A menu's first option (or the one with :default? true) is its resting
   state; the button lights up while another one is picked. :show-value?
   renders the picked label instead of the icon. A custom button with a
   built-in id (e.g. :quality) replaces the built-in.

   The runtime reports through a `change` CustomEvent on the root
   (detail {kind id value on}); :on-select :on-toggle :on-action :on-next
   wire it on Replicant and Squint. Hiccup pages listen for it themselves."
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.util :as util]))

(def hls-script-url
  "hls.js from the jsDelivr CDN. Load it before ui-runtime.js plays an HLS
   source; Safari plays .m3u8 natively without it."
  "https://cdn.jsdelivr.net/npm/hls.js@1")

(def builtin-controls
  "Control ids the runtime implements itself."
  #{"subtitles" "quality" "speed" "autonext" "pip" "fullscreen"})

(def ^:private builtin-labels
  {"subtitles" ["Subtitles" :captions]
   "quality" ["Quality" :settings]
   "speed" ["Speed" :gauge]
   "autonext" ["Autoplay next" :list-video]
   "pip" ["Picture in picture" :picture-in-picture-2]
   "fullscreen" ["Fullscreen" :maximize]})

(defn video-player-class-list
  "CSS classes for the root. :fit :fill makes the player fill its parent
   (full-page players) instead of keeping 16:9 with rounded corners.
   :variant :plain drops the glass: controls sit straight on a gradient,
   popovers and cards are solid."
  [{:keys [fit variant]}]
  (cond-> ["video-player"]
    (= "fill" (some-> fit util/kw-name)) (conj "video-player-fill")
    (= "plain" (some-> variant util/kw-name)) (conj "video-player-plain")))

(defn video-player-classes
  "Space-joined class string for the root."
  [opts]
  (str/join " " (video-player-class-list opts)))

;; ── Helpers ─────────────────────────────────────────────────────────

(defn- cls
  "An inner element's class attribute in the target's shape."
  [s]
  #?(:squint s
     :cljs   (str/split s #" ")
     :clj    s))

(defn- value-str
  "Option values travel through the DOM as strings (kw-name is identity
   on squint, so numbers need the explicit str)."
  [v]
  (if (nil? v) "" (str (util/kw-name v))))

(defn- bool-str [b] (if b "true" "false"))

(defn- ic
  ([n] (ic n nil))
  ([n extra]
   (icon/icon {:icon-name n :class (str "video-player-icon" (when extra (str " " extra)))})))

(defn- filled [n extra]
  (icon/icon {:icon-name n :filled true :class (str "video-player-icon " extra)}))

(defn- default-index
  "Index of the option marked :default?, else 0."
  [options]
  (or (first (keep-indexed (fn [i o] (when (:default? o) i)) options)) 0))

(defn- default-controls
  "Built-in menus, the custom buttons, then autonext, pip, fullscreen. A
   custom button with a built-in id keeps the built-in's place."
  [buttons next]
  (vec (distinct (concat ["subtitles" "quality" "speed"]
                         (map (comp util/kw-name :id) buttons)
                         (when next ["autonext"])
                         ["pip" "fullscreen"]))))

;; ── Pieces ──────────────────────────────────────────────────────────

(defn- panel
  "A popover above its trigger. Built-in panels are empty: the runtime
   fills them from the stream (levels, text tracks, rates)."
  [id label & options]
  (into [:div {:class (cls "video-player-panel video-player-glass")
               :data-vp-panel id :role "menu" :aria-label label :hidden true}
         [:div {:class (cls "video-player-panel-title")} label]]
        options))

(defn- option [{:keys [value label hint]} checked? default?]
  [:button (cond-> {:type "button" :class (cls "video-player-option")
                    :role "menuitemradio" :aria-checked (bool-str checked?)
                    :data-vp-value (value-str value)}
             default? (assoc :data-vp-default "true"))
   [:span {:class (cls "video-player-option-label")} label]
   (when hint [:span {:class (cls "video-player-option-hint")} hint])
   (ic :check "video-player-check")])

(defn- menu-button
  "Trigger + panel for a custom menu."
  [{:keys [id icon label options value show-value?]}]
  (let [id (util/kw-name id)
        d (default-index options)
        picked (or (first (keep-indexed (fn [i o] (when (= (value-str (:value o)) (value-str value)) i))
                                        options))
                   d)
        btn-class (if show-value? "video-player-pill" "video-player-btn")]
    [[:button {:type "button" :class (cls btn-class) :data-vp-open id
               :aria-haspopup "menu" :aria-expanded "false" :aria-label label
               :aria-pressed (bool-str (not= picked d)) :data-tooltip label}
      (if show-value?
        [:span {:data-vp-summary id} (:label (nth options picked nil))]
        (ic (or icon :settings)))]
     (into [:div {:class (cls "video-player-panel video-player-glass")
                  :data-vp-panel id :role "menu" :aria-label label :hidden true}
            [:div {:class (cls "video-player-panel-title")} label]]
           (map-indexed (fn [i o] (option o (= i picked) (= i d))) options))]))

(defn- control
  "[button panel-or-nil] for one control id."
  [id custom {:keys [next autonext?]}]
  (if-let [b (get custom id)]
    (case (some-> (:type b) util/kw-name)
      "toggle" [[:button {:type "button" :class (cls "video-player-btn") :data-vp-toggle id
                          :aria-pressed (bool-str (:pressed? b)) :aria-label (:label b) :data-tooltip (:label b)}
                 (ic (or (:icon b) :check))]]
      "action" [[:button {:type "button" :class (cls "video-player-btn") :data-vp-action id
                          :aria-label (:label b) :data-tooltip (:label b)}
                 (ic (or (:icon b) :settings))]]
      (menu-button b))
    (let [[label icn] (get builtin-labels id)]
      (case id
        "subtitles" [[:button {:type "button" :class (cls "video-player-btn") :data-vp-open id :data-vp-builtin id
                               :aria-haspopup "menu" :aria-expanded "false" :aria-pressed "false"
                               :aria-label label :data-tooltip label :hidden true}
                      (ic icn)]
                     (panel id label)]
        "quality"   [[:button {:type "button" :class (cls "video-player-pill") :data-vp-open id :data-vp-builtin id
                               :aria-haspopup "menu" :aria-expanded "false" :aria-label label :data-tooltip label :hidden true}
                      [:span {:data-vp-summary id} "Auto"]]
                     (panel id label)]
        "speed"     [[:button {:type "button" :class (cls "video-player-btn") :data-vp-open id :data-vp-builtin id
                               :aria-haspopup "menu" :aria-expanded "false" :aria-pressed "false"
                               :aria-label label :data-tooltip label}
                      (ic icn)]
                     (panel id label)]
        "autonext"  (when next
                      [[:button {:type "button" :class (cls "video-player-pill") :data-vp-toggle id
                                 :aria-pressed (bool-str (not (false? autonext?))) :aria-label label :data-tooltip label}
                        (ic icn) [:span {:class (cls "video-player-pill-label")} "Auto"]]])
        "pip"        [[:button {:type "button" :class (cls "video-player-btn") :data-vp-act "pip"
                                :aria-label label :data-tooltip label :hidden true}
                       (ic icn)]]
        "fullscreen" [[:button {:type "button" :class (cls "video-player-btn") :data-vp-act "fullscreen"
                                :aria-label label :data-tooltip "Fullscreen (f), Alt+click: fill the window"}
                       (ic :maximize "video-player-i-enter") (ic :minimize "video-player-i-exit")]]
        nil))))

(defn- play-button [extra]
  [:button {:type "button" :class (cls (str "video-player-btn " extra)) :data-vp-act "play"
            :aria-label "Play" :data-tooltip "Play (space)"}
   (filled :play "video-player-i-play") (filled :pause "video-player-i-pause")])

(defn- segment-style
  "A segment's start and end in seconds, as the custom properties the CSS
   divides by the runtime's --vp-duration."
  [start end]
  #?(:squint {"--vp-seg-start" start "--vp-seg-end" end}
     :cljs   {:--vp-seg-start start :--vp-seg-end end}
     :clj    (str "--vp-seg-start: " start "; --vp-seg-end: " end ";")))

(defn- segment
  "One marked stretch of the seek bar."
  [{:keys [start end label kind]}]
  [:i (cond-> {:class (cls "video-player-segment")
               :data-vp-segment (if kind (util/kw-name kind) "")
               :style (segment-style start end)}
        label (assoc :data-label label))])

(defn- up-next [{:keys [title thumb]}]
  [:div {:class (cls "video-player-upnext video-player-glass") :data-vp "upnext" :hidden true}
   [:div {:class (cls "video-player-upnext-thumb")}
    (when thumb [:img {:src thumb :alt ""}])
    [:i {:class (cls "video-player-upnext-progress")}]]
   [:div {:class (cls "video-player-upnext-kicker")}
    "Next episode in " [:span {:data-vp "upnext-secs"} "10"] " s"]
   [:div {:class (cls "video-player-upnext-title")} title]
   [:div {:class (cls "video-player-upnext-actions")}
    [:button {:type "button" :data-vp-act "upnext-cancel"} "Not now"]
    [:button {:type "button" :class (cls "video-player-upnext-play") :data-vp-act "next"} "Play"]]])

;; ── Component ───────────────────────────────────────────────────────

(defn- js-get
  "A property of a JS object by its string name, so advanced compilation
   can't rename it. Browser targets only."
  [o k]
  #?(:squint (aget o k)
     :cljs   (aget o k)
     :clj    nil))

(defn- change-handler
  "One handler for the runtime's change event, fanned out to the
   per-kind callbacks. Custom menu values map back to the option's
   original value (keyword, number) by their string form."
  [buttons {:keys [on-select on-toggle on-action on-next]}]
  (fn [e]
    (when-let [d (js-get e "detail")]
      (let [kind (js-get d "kind") id (js-get d "id")]
        (cond
          (= kind "select")
          (when on-select
            (let [b (some (fn [b] (when (= (util/kw-name (:id b)) id) b)) buttons)
                  v (js-get d "value")
                  orig (if b
                         (some (fn [o] (when (= (value-str (:value o)) (str v)) (:value o))) (:options b))
                         v)]
              (on-select id orig)))
          (= kind "toggle") (when on-toggle (on-toggle id (js-get d "on")))
          (= kind "action") (when on-action (on-action id))
          (= kind "next")   (when on-next (on-next)))))))

(defn video-player
  "Render a video player. The runtime takes over once it is in the DOM.

   Props:
     :src        - media URL (required). A .m3u8 plays through hls.js.
     :hls?       - force HLS handling for a source without .m3u8 in it
     :poster     - poster image URL
     :title      - shown in the top chip and next to the transport
     :back       - {:href url :label \"Back\"} adds a back button to the chip
     :start-at   - seconds to start from (resume)
     :autoplay?  - try to start playing (stays paused when the browser refuses)
     :tracks     - [{:src vtt :label \"English\" :lang \"en\" :default? true}]
     :next       - {:title :thumb :href} the next item: a Next button, the
                   up-next card and the :autonext toggle. With :href the
                   runtime navigates there; else only :on-next fires.
     :autonext?  - initial state of autoplay-next (default true)
     :segments   - [{:start 30 :end 75 :label \"Sponsor\" :kind :sponsor}]
                   stretches marked on the seek bar (seconds); the seek
                   tooltip names the one under the pointer. :kind lands in
                   data-vp-segment for per-kind colours. A host that only
                   learns them later calls the controller's segments.
     :controls   - ids in the right capsule, in order (default: subtitles
                   quality speed, every :buttons id, autonext pip fullscreen)
     :buttons    - custom controls, see the namespace doc
     :fit        - :contain (default, 16:9) or :fill (fill the parent)
     :variant    - :glass (default, blurred capsules) or :plain (controls on
                   a gradient, solid popovers, no blur)
     :on-select  - (fn [id value]) a menu option was picked
     :on-toggle  - (fn [id on?]) a toggle flipped (incl. :autonext)
     :on-action  - (fn [id]) an action button was pressed
     :on-next    - (fn []) Next pressed or the up-next countdown ran out
     :class      - extra classes on the root
     :attrs      - extra attributes on the root"
  [{:keys [src hls? poster title back start-at autoplay? tracks next autonext? segments
           controls buttons fit variant on-select on-toggle on-action on-next class attrs]}]
  (let [custom (into {} (map (fn [b] [(util/kw-name (:id b)) b]) buttons))
        ids (map util/kw-name (or controls (default-controls buttons next)))
        parts (keep (fn [id] (control id custom {:next next :autonext? autonext?})) ids)
        handler (when (or on-select on-toggle on-action on-next)
                  (change-handler buttons {:on-select on-select :on-toggle on-toggle
                                           :on-action on-action :on-next on-next}))
        root-attrs (cond-> {:data-video-player "" :data-src src :tabindex "0"
                            :role "region" :aria-label (or title "Video player")
                            :data-paused ""
                            :data-autonext (bool-str (and next (not (false? autonext?))))}
                     hls? (assoc :data-hls "true")
                     start-at (assoc :data-start-at (str start-at))
                     autoplay? (assoc :data-autoplay "true")
                     (:href next) (assoc :data-next-href (:href next))
                     handler #?(:squint (assoc :on-change handler)
                                :cljs   (assoc :on {:change handler})
                                :clj    identity))]
    [:div (util/class-attrs (video-player-class-list {:fit fit :variant variant}) class (merge root-attrs attrs))
     (into [:video (cond-> {:class (cls "video-player-media") :playsinline true :preload "metadata"}
                     poster (assoc :poster poster))]
           (map (fn [{:keys [src label lang default?]}]
                  [:track (cond-> {:kind "subtitles" :src src :label label}
                            lang (assoc :srclang lang)
                            default? (assoc :default true))])
                tracks))
     [:div {:class (cls "video-player-surface") :data-vp "surface"}]
     [:div {:class (cls "video-player-shade video-player-ctrl")}]
     (when (or title back)
       [:div {:class (cls "video-player-top video-player-ctrl")}
        [:div {:class (cls "video-player-chip video-player-glass")}
         (when back
           [:a {:class (cls "video-player-btn") :href (:href back) :aria-label (or (:label back) "Back")}
            (ic :arrow-left)])
         (when title [:span {:class (cls "video-player-chip-title")} title])]])
     [:div {:class (cls "video-player-center video-player-ctrl")}
      [:button {:type "button" :class (cls "video-player-btn video-player-glass video-player-skip")
                :data-vp-act "back" :aria-label "Back 10 seconds" :data-tooltip "Back 10 seconds"}
       (ic :rotate-ccw)]
      (play-button "video-player-glass video-player-big")
      [:button {:type "button" :class (cls "video-player-btn video-player-glass video-player-skip")
                :data-vp-act "forward" :aria-label "Forward 10 seconds" :data-tooltip "Forward 10 seconds"}
       (ic :rotate-cw)]]
     [:div {:class (cls "video-player-caption") :data-vp "caption"}]
     [:div {:class (cls "video-player-spinner")}]
     [:div {:class (cls "video-player-toast") :data-vp "toast" :role "status" :aria-live "polite"}]
     [:div {:class (cls "video-player-reconnect video-player-glass") :role "status" :aria-live "polite"}
      [:i {:class (cls "video-player-reconnect-spinner")}]
      [:span "Reconnecting…"]]
     [:button {:type "button" :class (cls "video-player-unmute video-player-glass") :data-vp-act "unmute"}
      (ic :volume-x) "Tap to unmute"]
     (when next (up-next next))
     [:div {:class (cls "video-player-dock video-player-ctrl")}
      [:div {:class (cls "video-player-scrub-row")}
       [:span {:class (cls "video-player-time") :data-vp "time"} "0:00"]
       [:div {:class (cls "video-player-scrub") :data-vp "scrub" :role "slider" :tabindex "0"
              :aria-label "Seek" :aria-valuemin "0" :aria-valuemax "0" :aria-valuenow "0"}
        [:div {:class (cls "video-player-track")}
         [:i {:class (cls "video-player-buffered")}]
         [:i {:class (cls "video-player-played")}]
         (into [:span {:class (cls "video-player-segments") :data-vp "segments"}]
               (map segment segments))]
        [:span {:class (cls "video-player-tip") :data-vp "tip"}]]
       [:span {:class (cls "video-player-time") :data-vp "left"} "-0:00"]]
      [:div {:class (cls "video-player-row")}
             [:div {:class (cls "video-player-capsule video-player-glass")}
              (play-button nil)
              (when next
                [:button {:type "button" :class (cls "video-player-btn") :data-vp-act "next"
                          :aria-label "Next" :data-tooltip "Next (n)"}
                 (filled :skip-forward "video-player-i-next")])
              [:div {:class (cls "video-player-volume")}
               [:button {:type "button" :class (cls "video-player-btn") :data-vp-act "mute"
                         :aria-label "Mute" :data-tooltip "Mute (m)"}
                (ic :volume-2 "video-player-i-vol") (ic :volume-x "video-player-i-muted")]
               [:div {:class (cls "video-player-volume-track") :data-vp "volume" :role "slider" :tabindex "0"
                      :aria-label "Volume" :aria-valuemin "0" :aria-valuemax "100" :aria-valuenow "100"}
                [:i]]]]
             (when title [:span {:class (cls "video-player-now")} title])
             [:span {:class (cls "video-player-spacer")}]
             (into [:div {:class (cls "video-player-capsule video-player-glass")}]
                   (map first parts))]]
     (into [:div {:class (cls "video-player-panels")}]
           (keep second parts))]))
