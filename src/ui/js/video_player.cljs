(ns video-player
  "Runtime for ui.video-player.

   Self-initializing: every [data-video-player] in the document becomes a
   player at load and whenever one is rendered later (MutationObserver);
   changing the root's data-src loads the new source, removing the root
   tears hls.js down.

   The runtime owns playback state and writes it to the DOM only through
   attributes the render never changes (root flags, aria-checked of
   built-in options, CSS variables), so a Replicant or Squint re-render
   doesn't fight it. State flags on the root:

     data-paused data-idle data-loading data-muted data-fullscreen
     data-panel-open data-upnext data-error data-autoplay-muted

   Autoplay (data-autoplay, or the controller's play) falls back to muted
   playback when the browser refuses sound, with a Tap to unmute pill.

   Sources: a .m3u8 (or data-hls) goes through window.Hls when the page
   loaded hls.js, else through native HLS (Safari); anything else is set
   as the video's src.

   Reports to the host with a non-bubbling `change` CustomEvent on the root:
   detail {kind: select|toggle|action|next, id, value, on}.

   window.__uiVideoPlayer(root) returns the controller
   {play pause seek toast load video}.")

(def ^:private players (js/Map.))
(def ^:private idle-ms 2600)
(def ^:private upnext-secs 10)
(def ^:private check-svg
  "<svg class=\"icon video-player-icon video-player-check\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" aria-hidden=\"true\"><path d=\"M20 6 9 17l-5-5\"/></svg>")
(def ^:private volume-key "ui-video-player:volume")

;; ── Small helpers ───────────────────────────────────────────────────

(defn- q [el sel] (.querySelector el sel))
(defn- qa [el sel] (js/Array.from (.querySelectorAll el sel)))

(defn- flag! [el k on?]
  (if on? (.setAttribute el k "") (.removeAttribute el k)))

(defn- esc [s]
  (-> (str s)
      (.replaceAll "&" "&amp;") (.replaceAll "<" "&lt;")
      (.replaceAll ">" "&gt;") (.replaceAll "\"" "&quot;")))

(defn- pad2 [n] (if (< n 10) (str "0" n) (str n)))

(defn- fmt [t]
  (if (not (js/isFinite t))
    "0:00"
    (let [t (js/Math.max 0 (js/Math.floor t))
          h (js/Math.floor (/ t 3600))
          m (js/Math.floor (/ (mod t 3600) 60))
          s (mod t 60)]
      (if (pos? h)
        (str h ":" (pad2 m) ":" (pad2 s))
        (str m ":" (pad2 s))))))

(defn- clamp [v lo hi] (js/Math.max lo (js/Math.min hi v)))

(defn- emit! [p detail]
  (.dispatchEvent (.-root p) (js/CustomEvent. "change" #js {:detail detail})))

(defn- text-tracks [p]
  (.filter (js/Array.from (.. p -video -textTracks))
           (fn [t] (or (= (.-kind t) "subtitles") (= (.-kind t) "captions")))))

(defn- levels [p]
  (if-let [h (.-hls p)] (or (.-levels h) #js []) #js []))

;; ── Toast ───────────────────────────────────────────────────────────

(defn- toast! [p msg]
  (when-let [n (q (.-root p) "[data-vp=toast]")]
    (set! (.-textContent n) msg)
    (.remove (.-classList n) "is-on")
    (.-offsetWidth n)
    (.add (.-classList n) "is-on")
    (js/clearTimeout (.-toastT p))
    (set! (.-toastT p) (js/setTimeout (fn [] (.remove (.-classList n) "is-on")) 1600))))

;; ── Built-in menus ──────────────────────────────────────────────────

(def ^:private rates [0.5 0.75 1 1.25 1.5 2])

(defn- rate-label [r] (if (= r 1) "Normal" (str r "×")))

(defn- builtin-options
  "Array of {value label hint default} for a built-in menu."
  [p id]
  (case id
    "quality"
    (let [lv (levels p)
          h (.-hls p)
          cur (when h (aget lv (.-currentLevel h)))
          auto? (or (not h) (.-autoLevelEnabled h))
          rows (-> (.map lv (fn [l i] #js {:value i :height (.-height l)}))
                   (.sort (fn [a b] (- (.-height b) (.-height a)))))]
      (.concat #js [#js {:value -1 :label "Auto" :default true
                         :hint (when (and auto? cur) (str (.-height cur) "p"))}]
               (.map rows (fn [r] #js {:value (.-value r) :label (str (.-height r) "p")
                                       :hint (when (>= (.-height r) 720) "HD")}))))
    "subtitles"
    (.concat #js [#js {:value -1 :label "Off" :default true}]
             (.map (text-tracks p) (fn [t i] #js {:value i :label (or (not-empty (.-label t))
                                                                       (not-empty (.-language t))
                                                                       (str "Track " (inc i)))})))
    "speed"
    (.map (into-array rates) (fn [r] #js {:value r :label (rate-label r) :default (= r 1)}))
    #js []))

(defn- builtin-value [p id]
  (case id
    "quality" (let [h (.-hls p)] (if (or (not h) (.-autoLevelEnabled h)) -1 (.-currentLevel h)))
    "subtitles" (.-sub p)
    "speed" (.. p -video -playbackRate)
    nil))

(defn- builtin-summary [p id]
  (case id
    "quality" (let [h (.-hls p) cur (when h (aget (levels p) (.-currentLevel h)))]
                (cond (not cur) "Auto"
                      (.-autoLevelEnabled h) (str "Auto · " (.-height cur) "p")
                      :else (str (.-height cur) "p")))
    nil))

(declare sync! draw-cue!)

(defn- set-sub! [p i say?]
  (let [ts (text-tracks p)]
    (.forEach ts (fn [t j] (set! (.-mode t) (if (= j i) "hidden" "disabled"))))
    (set! (.-sub p) i)
    (when (>= i 0) (set! (.-lastSub p) i))
    (draw-cue! p)
    (when say?
      (toast! p (if (< i 0) "Subtitles off" (str "Subtitles: " (.-label (aget ts i))))))))

(defn- builtin-select! [p id v]
  (case id
    "quality" (when-let [h (.-hls p)]
                (set! (.-currentLevel h) v)
                (toast! p (if (< v 0) "Quality: Auto" (str "Quality: " (.-height (aget (levels p) v)) "p"))))
    "subtitles" (set-sub! p v true)
    "speed" (do (set! (.. p -video -playbackRate) v)
                (toast! p (str "Speed: " (rate-label v))))
    nil))

(defn- fill-panel!
  "Render a built-in panel's options (keeps its title)."
  [p panel id]
  (let [cur (builtin-value p id)
        html (.join (.map (builtin-options p id)
                          (fn [o]
                            (str "<button type=\"button\" class=\"video-player-option\" role=\"menuitemradio\""
                                 " aria-checked=\"" (= (.-value o) cur) "\""
                                 " data-vp-value=\"" (esc (.-value o)) "\""
                                 (when (.-default o) " data-vp-default=\"true\"") ">"
                                 "<span class=\"video-player-option-label\">" (esc (.-label o)) "</span>"
                                 (when (.-hint o) (str "<span class=\"video-player-option-hint\">" (esc (.-hint o)) "</span>"))
                                 check-svg "</button>")))
                    "")
        title (q panel ".video-player-panel-title")]
    (set! (.-innerHTML panel) "")
    (when title (.appendChild panel title))
    (.insertAdjacentHTML panel "beforeend" html)))

;; ── Panels ──────────────────────────────────────────────────────────

(defn- place! [p panel trigger]
  (let [r (.getBoundingClientRect (.-root p))
        b (.getBoundingClientRect trigger)
        w (.-offsetWidth panel)
        left (- (+ (.-left b) (/ (.-width b) 2)) (/ w 2) (.-left r))]
    (set! (.. panel -style -bottom) (str (+ (- (.-bottom r) (.-top b)) 12) "px"))
    (set! (.. panel -style -left) (str (clamp left 8 (- (.-width r) w 8)) "px"))))

(defn- arm-hide! [p]
  (js/clearTimeout (.-hideT p))
  (set! (.-hideT p)
        (js/setTimeout (fn []
                         (when (and (not (.. p -video -paused)) (not (.-open p)))
                           (flag! (.-root p) "data-idle" true)))
                       idle-ms)))

(defn- show-ui! [p]
  (flag! (.-root p) "data-idle" false)
  (arm-hide! p))

(defn- close-panel! [p]
  (when-let [o (.-open p)]
    (set! (.-hidden (.-panel o)) true)
    (.setAttribute (.-trigger o) "aria-expanded" "false")
    (set! (.-open p) nil)
    (flag! (.-root p) "data-panel-open" false)
    (arm-hide! p)))

(defn- open-panel! [p id trigger]
  (let [panel (q (.-root p) (str "[data-vp-panel=\"" id "\"]"))
        same? (and (.-open p) (identical? (.. p -open -panel) panel))]
    (close-panel! p)
    (when (and panel (not same?))
      (when (.hasAttribute trigger "data-vp-builtin") (fill-panel! p panel id))
      (set! (.-hidden panel) false)
      (place! p panel trigger)
      (.setAttribute trigger "aria-expanded" "true")
      (set! (.-open p) #js {:panel panel :trigger trigger :id id})
      (flag! (.-root p) "data-panel-open" true)
      (show-ui! p)
      (when-let [c (or (q panel "[aria-checked=true]") (q panel ".video-player-option"))]
        (.focus c #js {:preventScroll true})))))

(defn- refresh-trigger!
  "Pressed state (+ summary label) of a menu trigger from its panel."
  [p id]
  (let [root (.-root p)
        trigger (q root (str "[data-vp-open=\"" id "\"]"))
        panel (q root (str "[data-vp-panel=\"" id "\"]"))]
    (when (and trigger panel)
      (if (.hasAttribute trigger "data-vp-builtin")
        (let [opts (builtin-options p id)
              cur (builtin-value p id)
              picked (.find opts (fn [o] (= (.-value o) cur)))]
          (.setAttribute trigger "aria-pressed" (str (boolean (and picked (not (.-default picked))))))
          (when-let [s (q trigger "[data-vp-summary]")]
            (set! (.-textContent s) (or (builtin-summary p id) (when picked (.-label picked)) ""))))
        (let [checked (q panel "[aria-checked=true]")
              dflt (or (q panel "[data-vp-default]") (q panel ".video-player-option"))]
          (.setAttribute trigger "aria-pressed" (str (boolean (and checked (not (identical? checked dflt))))))
          (when-let [s (q trigger "[data-vp-summary]")]
            (when checked
              (set! (.-textContent s) (.-textContent (q checked ".video-player-option-label"))))))))))

(defn- pick! [p panel opt]
  (let [id (.-id (.-open p))
        trigger (.-trigger (.-open p))
        raw (.getAttribute opt "data-vp-value")]
    (if (.hasAttribute trigger "data-vp-builtin")
      (let [v (js/parseFloat raw)]
        (builtin-select! p id v)
        (emit! p #js {:kind "select" :id id :value v}))
      (do
        (.forEach (qa panel ".video-player-option")
                  (fn [o] (.setAttribute o "aria-checked" (str (identical? o opt)))))
        (emit! p #js {:kind "select" :id id :value raw})))
    (close-panel! p)
    (.focus trigger #js {:preventScroll true})
    (sync! p)))

;; ── Subtitles: hidden tracks, cues drawn by us ──────────────────────

(defn- draw-cue! [p]
  (when-let [box (q (.-root p) "[data-vp=caption]")]
    (let [t (aget (text-tracks p) (.-sub p))
          cues (when t (.-activeCues t))
          text (when (and cues (pos? (.-length cues)))
                 (.join (.map (js/Array.from cues) (fn [c] (.-text c))) "\n"))]
      (set! (.-innerHTML box)
            (if text
              (str "<span>" (.replaceAll (esc (.replace text (js/RegExp. "<[^>]+>" "g") "")) "\n" "<br>") "</span>")
              "")))))

(defn- wire-track! [p t]
  (when-not (.-__vp t)
    (set! (.-__vp t) true)
    (.addEventListener t "cuechange" (fn [] (draw-cue! p)))))

(defn- init-tracks!
  "Take over the text tracks: draw cues ourselves, honour a default track."
  [p]
  (let [ts (text-tracks p)]
    (.forEach ts (fn [t] (wire-track! p t)))
    (when (< (.-sub p) 0)
      (let [i (.findIndex ts (fn [t] (= (.-mode t) "showing")))]
        (if (>= i 0)
          (set-sub! p i false)
          (.forEach ts (fn [t] (set! (.-mode t) "disabled"))))))
    (when-let [b (q (.-root p) "[data-vp-builtin=subtitles]")]
      (set! (.-hidden b) (zero? (.-length ts))))))

;; ── Up next ─────────────────────────────────────────────────────────

(defn- has-next? [p] (some? (q (.-root p) "[data-vp=upnext]")))

(defn- go-next! [p]
  (set! (.-dismissed p) false)
  (emit! p #js {:kind "next"})
  (when-let [href (.. p -root -dataset -nextHref)]
    (set! (.-href js/location) href)))

(defn- upnext! [p]
  (when-let [card (q (.-root p) "[data-vp=upnext]")]
    (let [v (.-video p)
          d (or (.-duration v) 0)
          left (- d (.-currentTime v))
          show? (and (.-auto p) (not (.-dismissed p)) (js/isFinite d) (pos? d) (<= left upnext-secs))]
      (set! (.-hidden card) (not show?))
      (flag! (.-root p) "data-upnext" show?)
      (when show?
        (.setProperty (.-style card) "--vp-countdown" (- 1 (/ left upnext-secs)))
        (when-let [s (q card "[data-vp=upnext-secs]")]
          (set! (.-textContent s) (js/Math.ceil left)))))))

;; ── State → DOM ─────────────────────────────────────────────────────

(defn- sync! [p]
  (let [root (.-root p) v (.-video p)
        muted? (or (.-muted v) (zero? (.-volume v)))]
    (flag! root "data-paused" (.-paused v))
    (flag! root "data-muted" muted?)
    (when-not muted? (flag! root "data-autoplay-muted" false))
    (flag! root "data-fullscreen" (identical? (.-fullscreenElement js/document) root))
    (.forEach (qa root "[data-vp-act=play]")
              (fn [b] (let [l (if (.-paused v) "Play" "Pause")]
                        (.setAttribute b "aria-label" l)
                        (.setAttribute b "title" (str l " (space)")))))
    (when-let [b (q root "[data-vp-toggle=autonext]")]
      (.setAttribute b "aria-pressed" (str (.-auto p))))
    (when-let [vol (q root "[data-vp=volume]")]
      (let [x (if muted? 0 (.-volume v))]
        (.setProperty (.-style vol) "--vp-volume" x)
        (.setAttribute vol "aria-valuenow" (js/Math.round (* 100 x)))))
    (.forEach (qa root "[data-vp-open]") (fn [b] (refresh-trigger! p (.getAttribute b "data-vp-open"))))
    (when-let [o (.-open p)]
      (when (.hasAttribute (.-trigger o) "data-vp-builtin")
        (fill-panel! p (.-panel o) (.-id o))))
    (upnext! p)))

(defn- tick!
  "Time, scrubber and buffer on timeupdate/progress."
  [p]
  (let [root (.-root p) v (.-video p)
        d (if (js/isFinite (.-duration v)) (.-duration v) 0)
        t (.-currentTime v)
        scrub (q root "[data-vp=scrub]")]
    (when-let [n (q root "[data-vp=time]")] (set! (.-textContent n) (fmt t)))
    (when-let [n (q root "[data-vp=left]")] (set! (.-textContent n) (str "-" (fmt (- d t)))))
    (when (and scrub (pos? d))
      (when-not (.-dragging p)
        (.setProperty (.-style scrub) "--vp-played" (/ t d)))
      (.setAttribute scrub "aria-valuemax" (js/Math.round d))
      (.setAttribute scrub "aria-valuenow" (js/Math.round t))
      (.setAttribute scrub "aria-valuetext" (str (fmt t) " of " (fmt d)))
      (let [b (.-buffered v)]
        (dotimes [i (.-length b)]
          (when (<= (.start b i) (+ t 0.5))
            (.setProperty (.-style scrub) "--vp-buffered" (/ (.end b i) d))))))
    (upnext! p)))

;; ── Actions ─────────────────────────────────────────────────────────

(defn- toggle-play! [p]
  (let [v (.-video p)]
    (if (.-paused v)
      (let [r (.play v)] (when (and r (.-catch r)) (.catch r (fn [_]))))
      (.pause v))))

(defn- autoplay!
  "Play. When the browser refuses sound without a gesture in this document
   (a stream that starts long after the click that opened the page), play
   muted instead, which is always allowed, and show the unmute pill: the
   next tap on the player brings the sound back instead of pausing."
  [p]
  (let [v (.-video p)
        r (.play v)]
    (when (and r (.-catch r))
      (.catch r (fn [e]
                  (when (= "NotAllowedError" (.-name e))
                    (set! (.-muted v) true)
                    (flag! (.-root p) "data-autoplay-muted" true)
                    (let [r2 (.play v)] (when (and r2 (.-catch r2)) (.catch r2 (fn [_]))))))))))

(defn- unmute! [p]
  (let [v (.-video p)]
    (set! (.-muted v) false)
    (when (zero? (.-volume v)) (set! (.-volume v) 1))
    (flag! (.-root p) "data-autoplay-muted" false)))

(defn- auto-muted? [p] (.hasAttribute (.-root p) "data-autoplay-muted"))

(defn- seek-by! [p s]
  (let [v (.-video p)]
    (set! (.-currentTime v) (clamp (+ (.-currentTime v) s) 0 (or (.-duration v) 0)))))

(defn- set-volume! [p x]
  (let [v (.-video p) x (clamp x 0 1)]
    (set! (.-volume v) x)
    (set! (.-muted v) (zero? x))
    (try (.setItem js/localStorage volume-key (str x)) (catch :default _))))

(defn- fullscreen! [p]
  (let [root (.-root p) v (.-video p)]
    (cond
      (.-fullscreenElement js/document) (.exitFullscreen js/document)
      (.-requestFullscreen root) (.requestFullscreen root)
      (.-webkitEnterFullscreen v) (.webkitEnterFullscreen v))))

(defn- act! [p a]
  (let [v (.-video p)]
    (case a
      "play" (toggle-play! p)
      "back" (do (seek-by! p -10) (toast! p "−10 s"))
      "forward" (do (seek-by! p 10) (toast! p "+10 s"))
      "mute" (if (or (.-muted v) (zero? (.-volume v)))
               (unmute! p)
               (set! (.-muted v) true))
      "unmute" (unmute! p)
      "next" (go-next! p)
      "upnext-cancel" (do (set! (.-dismissed p) true) (upnext! p))
      "pip" (if (.-pictureInPictureElement js/document)
              (.exitPictureInPicture js/document)
              (when (.-requestPictureInPicture v) (.requestPictureInPicture v)))
      "fullscreen" (fullscreen! p)
      nil)
    (sync! p)))

(defn- toggle! [p btn id]
  (let [on? (not= "true" (.getAttribute btn "aria-pressed"))]
    (if (= id "autonext")
      (do (set! (.-auto p) on?)
          (set! (.-dismissed p) false)
          (toast! p (if on? "Autoplay is on" "Autoplay is off")))
      (.setAttribute btn "aria-pressed" (str on?)))
    (emit! p #js {:kind "toggle" :id id :on on?})
    (sync! p)))

;; ── Sliders (scrubber, volume) ──────────────────────────────────────

(defn- frac [el e]
  (let [r (.getBoundingClientRect el)]
    (clamp (/ (- (.-clientX e) (.-left r)) (.-width r)) 0 1)))

(defn- wire-scrub! [p scrub]
  (let [v (.-video p)
        tip (q scrub "[data-vp=tip]")]
    (.addEventListener scrub "pointermove"
                       (fn [e]
                         (let [f (frac scrub e)]
                           (.setProperty (.-style scrub) "--vp-pointer" f)
                           (when tip (set! (.-textContent tip) (fmt (* f (or (.-duration v) 0)))))
                           (when (.-dragging p) (.setProperty (.-style scrub) "--vp-played" f)))))
    (.addEventListener scrub "pointerdown"
                       (fn [e]
                         (set! (.-dragging p) true)
                         (.setPointerCapture scrub (.-pointerId e))
                         (.add (.-classList scrub) "is-dragging")
                         (.setProperty (.-style scrub) "--vp-played" (frac scrub e))))
    (let [end (fn [e]
                (when (.-dragging p)
                  (set! (.-dragging p) false)
                  (.remove (.-classList scrub) "is-dragging")
                  (set! (.-currentTime v) (* (frac scrub e) (or (.-duration v) 0)))))]
      (.addEventListener scrub "pointerup" end)
      (.addEventListener scrub "pointercancel" end))
    (.addEventListener scrub "keydown"
                       (fn [e]
                         (case (.-key e)
                           "ArrowLeft" (do (.preventDefault e) (.stopPropagation e) (seek-by! p -5))
                           "ArrowRight" (do (.preventDefault e) (.stopPropagation e) (seek-by! p 5))
                           "Home" (do (.preventDefault e) (set! (.-currentTime v) 0))
                           "End" (do (.preventDefault e) (set! (.-currentTime v) (or (.-duration v) 0)))
                           nil)))))

(defn- wire-volume! [p vol]
  (let [drag (fn [e] (set-volume! p (frac vol e)))]
    (.addEventListener vol "pointerdown"
                       (fn [e]
                         (set! (.-volDrag p) true)
                         (.setPointerCapture vol (.-pointerId e))
                         (drag e)))
    (.addEventListener vol "pointermove" (fn [e] (when (.-volDrag p) (drag e))))
    (.addEventListener vol "pointerup" (fn [_] (set! (.-volDrag p) false)))
    (.addEventListener vol "keydown"
                       (fn [e]
                         (let [x (.. p -video -volume)
                               k (.-key e)
                               step (cond (or (= k "ArrowLeft") (= k "ArrowDown")) -0.1
                                          (or (= k "ArrowRight") (= k "ArrowUp")) 0.1)]
                           (when step
                             (.preventDefault e)
                             (.stopPropagation e)
                             (set-volume! p (+ x step))))))))

;; ── Keyboard ────────────────────────────────────────────────────────

(defn- on-key! [p e]
  (let [k (.-key e)
        handled
        (cond
          (and (= k "Escape") (.-open p)) (do (close-panel! p) true)
          (.-open p) false
          (or (= k " ") (= k "k")) (do (toggle-play! p) true)
          (= k "f") (do (fullscreen! p) true)
          (= k "m") (do (act! p "mute") true)
          (= k "c") (do (when (pos? (.-length (text-tracks p)))
                          (set-sub! p (if (< (.-sub p) 0) (.-lastSub p) -1) true)
                          (sync! p))
                        true)
          (and (= k "n") (has-next? p)) (do (go-next! p) true)
          (= k "ArrowRight") (do (seek-by! p 5) (toast! p "+5 s") true)
          (= k "ArrowLeft") (do (seek-by! p -5) (toast! p "−5 s") true)
          (= k "ArrowUp") (do (set-volume! p (+ (.. p -video -volume) 0.1)) true)
          (= k "ArrowDown") (do (set-volume! p (- (.. p -video -volume) 0.1)) true)
          :else false)]
    (when handled
      (.preventDefault e)
      (show-ui! p))))

(defn- on-document-key
  "Keys go to the player that holds focus, or to the only player on the
   page while nothing else does."
  [e]
  (let [t (.-target e)]
    (when-not (or (.-defaultPrevented e) (.-ctrlKey e) (.-metaKey e) (.-altKey e)
                  (and (.-closest t) (.closest t "input,textarea,select,[contenteditable]")))
      (let [root (or (and (.-closest t) (.closest t "[data-video-player]"))
                     (when (and (or (identical? t js/document.body) (identical? t js/document.documentElement))
                                (= 1 (.-size players)))
                       (.-root (.-value (.next (.values players))))))]
        (when-let [p (and root (.get players root))]
          (if (and (= (.-key e) " ") (.closest t "button,a"))
            nil
            (on-key! p e)))))))

;; ── Source ──────────────────────────────────────────────────────────

(defn- destroy-hls! [p]
  (when-let [h (.-hls p)]
    (.destroy h)
    (set! (.-hls p) nil)))

(defn- load! [p]
  (destroy-hls! p)
  (let [root (.-root p) v (.-video p)
        src (or (.. root -dataset -src) "")
        start (js/parseFloat (or (.. root -dataset -startAt) "0"))
        start (if (js/isNaN start) 0 start)
        hls? (or (= "true" (.. root -dataset -hls)) (.test (js/RegExp. "\\.m3u8($|[?#])" "i") src))
        Hls (.-Hls js/window)
        autoplay? (= "true" (.. root -dataset -autoplay))
        play! (fn [] (when autoplay? (autoplay! p)))]
    (set! (.-src p) src)
    (set! (.-dismissed p) false)
    (set! (.-sub p) -1)
    (flag! root "data-error" false)
    ;; An autoplaying source shows the spinner from the start, not the big
    ;; play button; playing/canplay clear it.
    (flag! root "data-loading" (and autoplay? (not= "" src)))
    (cond
      ;; No source yet (a host still waiting for its stream) or cleared:
      ;; empty the video. Setting src to "" would load the page itself.
      (= "" src)
      (do (when-let [b (q root "[data-vp-builtin=quality]")] (set! (.-hidden b) true))
          (.removeAttribute v "src")
          (.load v))

      (and hls? Hls (.isSupported Hls))
      (let [h (Hls. #js {:startPosition (if (pos? start) start -1) :maxBufferLength 30})]
        (set! (.-hls p) h)
        (set! (.-fails p) 0)
        (.loadSource h src)
        (.attachMedia h v)
        (.on h (.. Hls -Events -MANIFEST_PARSED)
             (fn [] (when-let [b (q root "[data-vp-builtin=quality]")]
                      (set! (.-hidden b) (< (.-length (levels p)) 2)))
               (sync! p) (play!)))
        (.on h (.. Hls -Events -LEVEL_SWITCHED) (fn [] (sync! p)))
        (.on h (.. Hls -Events -FRAG_LOADED) (fn [] (set! (.-fails p) 0)))
        (.on h (.. Hls -Events -ERROR)
             (fn [_ d]
               (when (.-fatal d)
                 (set! (.-fails p) (inc (.-fails p)))
                 (cond
                   (> (.-fails p) 8) (do (flag! root "data-error" true)
                                         (toast! p (str "Playback error: " (.-details d))))
                   (= (.-type d) (.. Hls -ErrorTypes -MEDIA_ERROR)) (.recoverMediaError h)
                   :else (do (toast! p "Reconnecting…")
                             (js/setTimeout (fn [] (when (identical? (.-hls p) h)
                                                     (.startLoad h (.-currentTime v))))
                                            (js/Math.min 8000 (* 500 (.-fails p))))))))))

      :else
      (do
        (when-let [b (q root "[data-vp-builtin=quality]")] (set! (.-hidden b) true))
        (if (and hls? (= "" (.canPlayType v "application/vnd.apple.mpegurl")))
          (do (flag! root "data-error" true) (toast! p "This browser can't play HLS"))
          (do
            (set! (.-src v) src)
            (when (pos? start)
              (.addEventListener v "loadedmetadata" (fn [] (set! (.-currentTime v) start)) #js {:once true}))
            (play!)))))))

;; ── Init / teardown ─────────────────────────────────────────────────

(defn- init! [root]
  (let [v (q root "video")
        p #js {:root root :video v :hls nil :sub -1 :lastSub 0
               :auto (= "true" (.. root -dataset -autonext))
               :dismissed false :open nil :dragging false :volDrag false}]
    (.set players root p)
    (try (when-let [x (.getItem js/localStorage volume-key)]
           (let [x (js/parseFloat x)]
             (when-not (js/isNaN x) (set! (.-volume v) x) (set! (.-muted v) (zero? x)))))
         (catch :default _))
    ;; Clicks: one delegated listener per player.
    (.addEventListener root "click"
                       (fn [e]
                         (let [t (.closest (.-target e) "[data-vp-open],[data-vp-toggle],[data-vp-action],[data-vp-act],.video-player-option")]
                           (cond
                             (not t)
                             (cond (.-open p) (close-panel! p)
                                   (not (.closest (.-target e) "[data-vp=surface]")) nil
                                   (auto-muted? p) (unmute! p)
                                   :else (toggle-play! p))

                             (.hasAttribute t "data-vp-open")
                             (open-panel! p (.getAttribute t "data-vp-open") t)

                             (.contains (.-classList t) "video-player-option")
                             (pick! p (.closest t "[data-vp-panel]") t)

                             (.hasAttribute t "data-vp-toggle")
                             (toggle! p t (.getAttribute t "data-vp-toggle"))

                             (.hasAttribute t "data-vp-action")
                             (emit! p #js {:kind "action" :id (.getAttribute t "data-vp-action")})

                             :else (act! p (.getAttribute t "data-vp-act"))))))
    (.addEventListener root "dblclick"
                       (fn [e] (when (.closest (.-target e) "[data-vp=surface]") (fullscreen! p))))
    (.addEventListener js/document "pointerdown"
                       (fn [e]
                         (when-let [o (.-open p)]
                           (when-not (or (.contains (.-panel o) (.-target e))
                                         (.contains (.-trigger o) (.-target e)))
                             (close-panel! p)))))
    ;; Idle hiding.
    (.forEach #js ["pointermove" "pointerdown" "keydown" "focusin"]
              (fn [t] (.addEventListener root t (fn [] (show-ui! p)) #js {:passive true})))
    (.addEventListener root "pointerleave"
                       (fn [e] (when (and (= (.-pointerType e) "mouse") (not (.-paused v)) (not (.-open p)))
                                 (flag! root "data-idle" true))))
    (when-let [s (q root "[data-vp=scrub]")] (wire-scrub! p s))
    (when-let [s (q root "[data-vp=volume]")] (wire-volume! p s))
    (.forEach #js ["play" "pause" "volumechange" "ratechange"]
              (fn [t] (.addEventListener v t (fn [] (sync! p) (arm-hide! p)))))
    (.forEach #js ["timeupdate" "progress" "durationchange" "seeked"]
              (fn [t] (.addEventListener v t (fn [] (tick! p)))))
    (.addEventListener v "waiting" (fn [] (flag! root "data-loading" true)))
    (.forEach #js ["playing" "canplay" "pause"]
              (fn [t] (.addEventListener v t (fn [] (flag! root "data-loading" false)))))
    (.addEventListener v "loadedmetadata" (fn [] (init-tracks! p) (sync! p) (tick! p)))
    (.addEventListener (.-textTracks v) "addtrack" (fn [] (init-tracks! p) (sync! p)))
    (.addEventListener v "ended"
                       (fn [] (when (and (has-next? p) (.-auto p) (not (.-dismissed p)))
                                (go-next! p))))
    (when-let [b (q root "[data-vp-act=pip]")]
      (set! (.-hidden b) (not (.-pictureInPictureEnabled js/document))))
    (init-tracks! p)
    (load! p)
    (sync! p)
    (tick! p)))

(defn- teardown! [root]
  (when-let [p (.get players root)]
    (destroy-hls! p)
    (.delete players root)))

(defn- scan! []
  (.forEach (qa js/document "[data-video-player]")
            (fn [root]
              (if-let [p (.get players root)]
                (when (not= (.-src p) (or (.. root -dataset -src) ""))
                  (load! p) (sync! p))
                (init! root))))
  (.forEach (js/Array.from (.keys players))
            (fn [root] (when-not (.-isConnected root) (teardown! root)))))

(defn- boot! []
  (scan!)
  (.addEventListener js/document "keydown" on-document-key)
  (.addEventListener js/document "fullscreenchange"
                     (fn [] (.forEach players (fn [p] (close-panel! p) (sync! p)))))
  (let [raf (atom nil)
        obs (js/MutationObserver.
             (fn [_]
               (when-not @raf
                 (reset! raf (js/requestAnimationFrame (fn [] (reset! raf nil) (scan!)))))))]
    (.observe obs js/document.body
              #js {:childList true :subtree true
                   :attributes true :attributeFilter #js ["data-src"]})))

(aset js/window "__uiVideoPlayer"
      (fn [root]
        (when-let [p (.get players root)]
          #js {:video (.-video p)
               :play (fn [] (autoplay! p))
               :pause (fn [] (.pause (.-video p)))
               :seek (fn [t] (set! (.. p -video -currentTime) t))
               :toast (fn [msg] (toast! p msg))
               :load (fn [] (load! p) (sync! p))})))

(if (= "loading" (.-readyState js/document))
  (.addEventListener js/document "DOMContentLoaded" boot!)
  (boot!))
