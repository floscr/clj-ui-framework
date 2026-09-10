(ns dial-timeline
  "DialKit timeline — define animation clips in code, then scrub / tune their
   timing in a bottom dock. A vanilla port of the DialKit timeline for the
   clj-ui JS runtime.

     window.__uiDialTimeline(name, config, opts?) -> controller

   config : object mapping clip names -> clip defs
            {at, duration?, from, to, transition?, loop?}         single
            {at, from, steps:[{duration,to,transition?}], ...}    sequence
            {at, loop?, props:{name:{from,delay?,steps|to,...}}}  property tracks
            {at, duration?}                                       marker
            nested object of clips (no :at)                       group
   opts   : {id, persist, autoplay (true), loop (false | true | {from})}

   controller: live getters `values` (nested clip states + time/playing/
   duration), `time`, `playing`, `duration`; methods getValues, subscribe,
   play, pause, replay, seek(sec), destroy.

   Each clip state: {at, duration, progress, started, active, done, loop,
   step, from, to, current, animate, transition, css}. Bind `.current` into
   the UI for a scrub-responsive preview.

   Panel fields read via (:kw tl) / written via (aset tl \"kw\" v)."
  (:require [clojure.string :as str]))

;; ── helpers (kept local; mirror dial.cljs) ──────────────────────────

(defn- tof [v] (js* "typeof ~{}" v))
(defn- mk [tag class]
  (let [e (js/document.createElement tag)]
    (when class (set! (.-className e) class)) e))
(defn- add! [parent & children] (doseq [c children] (when c (.appendChild parent c))) parent)
(defn- txt! [e s] (set! (.-textContent e) (str s)) e)
(defn- on! [e ev f] (.addEventListener e ev f) e)
(defn- attr! [e k v] (.setAttribute e k v) e)
(defn- clamp [v lo hi] (Math/max lo (Math/min v hi)))

(defn- fmt-clock [sec]
  (let [s (Math/max 0 sec)
        whole (js/Math.floor s)
        tenths (js/Math.floor (* 10 (- s whole)))]
    (str whole "." tenths "s")))

(defn- cubic-bezier-y [e p]
  (let [t p mt (- 1 t)]
    (+ (* 3 mt mt t (aget e 1)) (* 3 mt t t (aget e 3)) (* t t t))))

(defn- sample-spring [visual-dur bounce t]
  (let [zeta  (clamp (- 1 bounce) 0.05 1)
        omega (/ (* 2 js/Math.PI) (Math/max 0.05 visual-dur))]
    (if (< zeta 1)
      (let [wd (* omega (js/Math.sqrt (- 1 (* zeta zeta))))]
        (- 1 (* (js/Math.exp (* (- zeta) omega t))
                (+ (js/Math.cos (* wd t))
                   (* (/ (* zeta omega) wd) (js/Math.sin (* wd t)))))))
      (- 1 (* (js/Math.exp (* (- omega) t)) (+ 1 (* omega t)))))))

;; ── Transition → interpolation factor + effective duration ──────────

(defn- transition-duration [tr fallback]
  (cond
    (nil? tr) fallback
    (= (aget tr "type") "easing") (or (aget tr "duration") 0.3)
    (aget tr "stiffness") (let [k (aget tr "stiffness") m (or (aget tr "mass") 1)]
                            (clamp (* 4 (js/Math.sqrt (/ m k))) 0.2 3))
    :else (or (aget tr "visualDuration") 0.5)))

(defn- ease-factor [tr p]
  (cond
    (nil? tr) p
    (= (aget tr "type") "easing")
    (cubic-bezier-y (or (aget tr "ease") #js [0.25 0.1 0.25 1]) p)
    (aget tr "stiffness")
    (sample-spring 0.5 0.25 (* p 1))
    :else
    (sample-spring (or (aget tr "visualDuration") 0.5) (or (aget tr "bounce") 0.2)
                   (* p 1))))

(defn- lerp-map [from to factor]
  (let [out #js {}
        keys (js/Array.from (js/Set. (.concat (js/Object.keys (or from #js {}))
                                              (js/Object.keys (or to #js {})))))]
    (doseq [k keys]
      (let [a (if (and from (not= (aget from k) js/undefined)) (aget from k) (aget to k))
            b (if (and to (not= (aget to k) js/undefined)) (aget to k) (aget from k))]
        (aset out k (+ a (* (- b a) factor)))))
    out))

;; ── Clip parsing ────────────────────────────────────────────────────

(declare parse-node)

(defn- clip? [v]
  (and (= (tof v) "object") (not (js/Array.isArray v))
       (or (not= (aget v "at") js/undefined)
           (aget v "from") (aget v "to") (aget v "steps") (aget v "props"))))

(defn- parse-clip [v]
  (let [at (or (aget v "at") 0)
        tr (aget v "transition")
        loop? (boolean (aget v "loop"))]
    (cond
      (aget v "props")
      (let [tracks #js {}
            names (js/Object.keys (aget v "props"))]
        (doseq [nm names]
          (let [tk (aget (aget v "props") nm)
                steps (or (aget tk "steps")
                          #js [#js {:duration (transition-duration (or (aget tk "transition") tr) 0.5)
                                    :to (aget tk "to") :transition (aget tk "transition")}])
                dur (reduce (fn [a s] (+ a (or (aget s "duration") 0.5))) 0 steps)]
            (aset tracks nm #js {:from (aget tk "from") :delay (or (aget tk "delay") 0)
                                 :steps steps :transition (or (aget tk "transition") tr) :dur dur})))
        (let [dur (reduce (fn [a nm] (Math/max a (+ (aget (aget tracks nm) "delay") (aget (aget tracks nm) "dur")))) 0 names)]
          #js {:kind "props" :at at :duration (or (aget v "duration") dur) :loop loop?
               :tracks tracks :transition tr}))

      (aget v "steps")
      (let [steps (aget v "steps")
            dur (reduce (fn [a s] (+ a (or (aget s "duration") 0.5))) 0 steps)]
        #js {:kind "sequence" :at at :duration (or (aget v "duration") dur) :loop loop?
             :from (aget v "from") :steps steps :transition tr})

      (or (aget v "from") (aget v "to"))
      #js {:kind "single" :at at
           :duration (or (aget v "duration") (transition-duration tr 0.5)) :loop loop?
           :from (aget v "from") :to (aget v "to") :transition tr}

      :else
      #js {:kind "marker" :at at :duration (or (aget v "duration") 0) :loop false})))

(defn- parse-node [nm v]
  (if (clip? v)
    #js {:name nm :clip (parse-clip v)}
    ;; group: nested clips
    #js {:name nm :group (mapv (fn [k] (parse-node k (aget v k))) (js/Object.keys v))}))

(defn- parse-config [config]
  (mapv (fn [k] (parse-node k (aget config k))) (js/Object.keys config)))

;; ── Sampling clip state at time t ───────────────────────────────────

(defn- clip-end [clip] (+ (aget clip "at") (aget clip "duration")))

(defn- sample-single [clip local]
  (let [dur (Math/max 0.0001 (aget clip "duration"))
        p (clamp (/ local dur) 0 1)
        f (ease-factor (aget clip "transition") p)]
    #js {:progress p
         :from (aget clip "from") :to (aget clip "to")
         :current (lerp-map (aget clip "from") (aget clip "to") f)
         :animate (if (>= p 1) (aget clip "to") (aget clip "from"))}))

(defn- sample-sequence [clip local]
  (let [steps (aget clip "steps")
        state (js/JSON.parse (js/JSON.stringify (or (aget clip "from") #js {})))]
    (loop [i 0 acc 0 cur state stepidx 0]
      (if (>= i (.-length steps))
        #js {:progress 1 :from (aget clip "from") :to cur :current cur :step (dec (.-length steps)) :animate cur}
        (let [s (aget steps i)
              d (or (aget s "duration") 0.5)
              to (or (aget s "to") cur)]
          (if (<= local (+ acc d))
            (let [lp (clamp (/ (- local acc) (Math/max 0.0001 d)) 0 1)
                  f (ease-factor (or (aget s "transition") (aget clip "transition")) lp)]
              #js {:progress (clamp (/ local (Math/max 0.0001 (aget clip "duration"))) 0 1)
                   :from (aget clip "from") :to to :step i
                   :current (lerp-map cur to f)
                   :animate (if (>= lp 1) to cur)})
            (recur (inc i) (+ acc d) (let [m (js/Object.assign #js {} cur)] (js/Object.assign m to)) i)))))))

(defn- sample-track [track local]
  (let [delay (aget track "delay")
        lt (- local delay)]
    (if (< lt 0)
      (aget track "from")
      (let [steps (aget track "steps")]
        (loop [i 0 acc 0 cur (aget track "from")]
          (if (>= i (.-length steps))
            cur
            (let [s (aget steps i) d (or (aget s "duration") 0.5) to (aget s "to")]
              (if (<= lt (+ acc d))
                (let [lp (clamp (/ (- lt acc) (Math/max 0.0001 d)) 0 1)
                      f (ease-factor (or (aget s "transition") (aget track "transition")) lp)]
                  (+ cur (* (- to cur) f)))
                (recur (inc i) (+ acc d) to)))))))))

(defn- sample-props [clip local]
  (let [tracks (aget clip "tracks")
        cur #js {} from #js {} to #js {}]
    (doseq [nm (js/Object.keys tracks)]
      (let [tk (aget tracks nm)
            steps (aget tk "steps")]
        (aset cur nm (sample-track tk local))
        (aset from nm (aget tk "from"))
        (aset to nm (aget (aget steps (dec (.-length steps))) "to"))))
    #js {:progress (clamp (/ local (Math/max 0.0001 (aget clip "duration"))) 0 1)
         :current cur :from from :to to :animate cur}))

(defn- sample-clip [clip t]
  (let [at (aget clip "at")
        dur (aget clip "duration")
        raw (- t at)
        started (>= t at)
        cyc (Math/max 0.0001 dur)
        local (if (aget clip "loop")
                (if (< raw 0) 0 (js/Math.min cyc (- raw (* cyc (js/Math.floor (/ raw cyc))))))
                (clamp raw 0 dur))
        active (and started (or (aget clip "loop") (<= t (clip-end clip))))
        done (and (not (aget clip "loop")) (> t (clip-end clip)))
        base (cond
               (= (aget clip "kind") "props") (sample-props clip local)
               (= (aget clip "kind") "sequence") (sample-sequence clip local)
               (= (aget clip "kind") "single") (sample-single clip local)
               :else #js {:progress (clamp (/ local cyc) 0 1) :current #js {}})
        css (let [tr (aget clip "transition")]
              (if (and tr (= (aget tr "type") "easing"))
                #js {:duration (str (aget clip "duration") "s")
                     :timingFunction (str "cubic-bezier(" (.join (or (aget tr "ease") #js [0.25 0.1 0.25 1]) ",") ")")}
                #js {:duration (str (aget clip "duration") "s") :timingFunction "ease"}))]
    (js/Object.assign base #js {:at at :duration dur :loop (if (aget clip "loop") "repeat" "off")
                                :started started :active active :done done
                                :transition (aget clip "transition") :css css})
    base))

;; ── Values snapshot ─────────────────────────────────────────────────

(defn- node-values [node t]
  (if (aget node "clip")
    (sample-clip (aget node "clip") t)
    (let [g #js {}]
      (doseq [child (aget node "group")]
        (aset g (aget child "name") (node-values child t)))
      g)))

(defn- total-duration [nodes]
  (letfn [(walk [node]
            (if (aget node "clip")
              (clip-end (aget node "clip"))
              (reduce (fn [a c] (Math/max a (walk c))) 0 (aget node "group"))))]
    (reduce (fn [a n] (Math/max a (walk n))) 0.0001 nodes)))

(defn- snapshot [tl]
  (let [t (:time tl)
        out #js {}]
    (doseq [node (:nodes tl)]
      (aset out (aget node "name") (node-values node t)))
    (aset out "time" t)
    (aset out "playing" (:playing tl))
    (aset out "duration" (:duration tl))
    out))

(defn- notify! [tl]
  (let [snap (snapshot tl)]
    (when-let [cb (:onChange tl)] (cb snap))
    (doseq [s (js/Array.from (:subs tl))] (s snap))))

;; ── Playback loop ───────────────────────────────────────────────────

(declare paint-dock!)

(defn- tick! [tl now]
  (let [last (:last tl)
        dt (if last (/ (- now last) 1000) 0)]
    (aset tl "last" now)
    (when (:playing tl)
      (let [t (+ (:time tl) dt)
            dur (:duration tl)
            lp (:loopOpt tl)]
        (cond
          (< t dur) (aset tl "time" t)
          (= lp true) (aset tl "time" (- t dur))
          (and lp (not= lp true) (not= lp false))
          (let [from (or (aget lp "from") 0)] (aset tl "time" (+ from (- t dur))))
          :else (do (aset tl "time" dur) (aset tl "playing" false)))))
    (notify! tl)
    (paint-dock! tl)
    (aset tl "raf" (js/requestAnimationFrame (fn [n] (tick! tl n))))))

(defn- play! [tl] (aset tl "playing" true))
(defn- pause! [tl] (aset tl "playing" false))
(defn- seek! [tl s] (aset tl "time" (clamp s 0 (:duration tl))) (notify! tl) (paint-dock! tl))
(defn- replay! [tl] (aset tl "time" 0) (aset tl "playing" true))

;; ── Dock UI ─────────────────────────────────────────────────────────

(def ^:private dock-el (atom nil))
(def ^:private timelines (atom #js []))

(defn- px-per-sec [tl]
  (let [ruler (:ruler tl)]
    (if ruler (/ (.-clientWidth ruler) (Math/max 0.0001 (* (:zoom tl) (:duration tl)))) 100)))

(defn- ensure-dock! []
  (or @dock-el
      (let [el (mk "div" "dial-timeline")]
        (attr! el "data-theme" "system")
        (.appendChild js/document.body el)
        (reset! dock-el el)
        el)))

(defn- build-clip-rows! [tl track-host]
  (set! (.-innerHTML track-host) "")
  (let [pps (px-per-sec tl)]
    (letfn [(render-node [node depth]
              (if (aget node "clip")
                (let [clip (aget node "clip")
                      row (mk "div" "dtl-row")
                      bar (mk "div" "dtl-clip")]
                  (set! (.. row -style -paddingLeft) (str (* depth 12) "px"))
                  (add! row (txt! (mk "span" "dtl-row-label") (aget node "name")))
                  (set! (.. bar -style -left) (str (* pps (aget clip "at")) "px"))
                  (set! (.. bar -style -width) (str (Math/max 8 (* pps (aget clip "duration"))) "px"))
                  (txt! bar (str (aget node "name") " " (fmt-clock (aget clip "duration"))))
                  ;; drag whole clip = change at; edge drag = resize
                  (let [st #js {:mode nil :sx 0 :at0 0 :dur0 0}]
                    (on! bar "pointerdown"
                         (fn [e] (.setPointerCapture bar (.-pointerId e))
                           (let [rect (.getBoundingClientRect bar)
                                 near-edge (> (.-clientX e) (- (.-right rect) 10))]
                             (aset st "mode" (if near-edge "resize" "move"))
                             (aset st "sx" (.-clientX e))
                             (aset st "at0" (aget clip "at"))
                             (aset st "dur0" (aget clip "duration")))))
                    (on! bar "pointermove"
                         (fn [e]
                           (when (aget st "mode")
                             (let [dx (/ (- (.-clientX e) (aget st "sx")) pps)]
                               (if (= (aget st "mode") "resize")
                                 (aset clip "duration" (Math/max 0.05 (+ (aget st "dur0") dx)))
                                 (aset clip "at" (Math/max 0 (+ (aget st "at0") dx))))
                               (aset tl "duration" (Math/max (:baseDuration tl) (total-duration (:nodes tl))))
                               (set! (.. bar -style -left) (str (* pps (aget clip "at")) "px"))
                               (set! (.. bar -style -width) (str (Math/max 8 (* pps (aget clip "duration"))) "px"))
                               (notify! tl)))))
                    (on! bar "pointerup" (fn [_] (aset st "mode" nil))))
                  (add! row bar)
                  (.appendChild track-host row))
                ;; group header + children
                (do
                  (let [hdr (mk "div" "dtl-group")]
                    (set! (.. hdr -style -paddingLeft) (str (* depth 12) "px"))
                    (txt! hdr (aget node "name"))
                    (.appendChild track-host hdr))
                  (doseq [child (aget node "group")]
                    (render-node child (inc depth))))))]
      (doseq [node (:nodes tl)] (render-node node 0)))))

(defn- paint-dock! [tl]
  (when (:playhead tl)
    (let [pps (px-per-sec tl)]
      (set! (.. (:playhead tl) -style -left) (str (* pps (:time tl)) "px"))
      (txt! (:clock tl) (fmt-clock (:time tl))))))

(defn- build-dock! [tl]
  (let [dock (ensure-dock!)
        panel (mk "div" "dtl-panel")
        bar   (mk "div" "dtl-toolbar")
        playb (mk "button" "dtl-btn")
        repb  (mk "button" "dtl-btn")
        clock (mk "span" "dtl-clock")
        name  (txt! (mk "span" "dtl-name") (:name tl))
        scroller (mk "div" "dtl-scroll")
        ruler (mk "div" "dtl-ruler")
        tracks (mk "div" "dtl-tracks")
        playhead (mk "div" "dtl-playhead")]
    (attr! playb "type" "button") (txt! playb "⏵")
    (attr! repb "type" "button") (txt! repb "↺")
    (aset tl "ruler" ruler)
    (aset tl "playhead" playhead)
    (aset tl "clock" clock)
    (aset tl "trackHost" tracks)
    (on! playb "click" (fn [_] (if (:playing tl) (pause! tl) (play! tl))
                         (txt! playb (if (:playing tl) "⏸" "⏵"))))
    (on! repb "click" (fn [_] (replay! tl) (txt! playb "⏸")))
    ;; ruler ticks
    (let [pps (px-per-sec tl)
          n (js/Math.ceil (:duration tl))]
      (dotimes [i (inc n)]
        (let [tick (mk "div" "dtl-tick")]
          (set! (.. tick -style -left) (str (* pps i) "px"))
          (txt! tick (str i "s"))
          (add! ruler tick))))
    ;; scrub on ruler
    (let [scrub (fn [e]
                  (let [rect (.getBoundingClientRect ruler)
                        pps (px-per-sec tl)
                        s (/ (+ (- (.-clientX e) (.-left rect)) (.-scrollLeft scroller)) pps)]
                    (seek! tl s)))
          st #js {:on false :wasPlaying false}]
      (on! ruler "pointerdown" (fn [e] (.setPointerCapture ruler (.-pointerId e))
                                 (aset st "on" true) (aset st "wasPlaying" (:playing tl))
                                 (pause! tl) (scrub e)))
      (on! ruler "pointermove" (fn [e] (when (aget st "on") (scrub e))))
      (on! ruler "pointerup" (fn [_] (aset st "on" false)
                               (when (aget st "wasPlaying") (play! tl))))
      ;; alt+drag zoom
      (on! ruler "wheel" (fn [e]
                           (when (.-altKey e)
                             (.preventDefault e)
                             (aset tl "zoom" (clamp (* (:zoom tl) (if (> (.-deltaY e) 0) 1.1 0.9)) 0.2 8))
                             (build-clip-rows! tl (:trackHost tl))))))
    (add! bar playb repb clock name)
    (add! scroller ruler tracks playhead)
    (add! panel bar scroller)
    (add! dock panel)
    (build-clip-rows! tl tracks)
    (paint-dock! tl)
    (aset tl "panelEl" panel)))

;; ── Controller / public API ─────────────────────────────────────────

(defn- controller [tl]
  (let [c #js {:getValues (fn [] (snapshot tl))
               :subscribe (fn [cb immediate]
                            (.push (:subs tl) cb)
                            (when (not= immediate false) (cb (snapshot tl)))
                            (fn [] (let [i (.indexOf (:subs tl) cb)] (when (>= i 0) (.splice (:subs tl) i 1)))))
               :play (fn [] (play! tl) js/undefined)
               :pause (fn [] (pause! tl) js/undefined)
               :replay (fn [] (replay! tl) js/undefined)
               :seek (fn [s] (seek! tl s) js/undefined)
               :destroy (fn []
                          (when (:raf tl) (js/cancelAnimationFrame (:raf tl)))
                          (when-let [p (:panelEl tl)] (.remove p))
                          js/undefined)}]
    (js/Object.defineProperty c "values" #js {:get (fn [] (snapshot tl))})
    (js/Object.defineProperty c "time" #js {:get (fn [] (:time tl))})
    (js/Object.defineProperty c "playing" #js {:get (fn [] (:playing tl))})
    (js/Object.defineProperty c "duration" #js {:get (fn [] (:duration tl))})
    c))

(defn use-timeline [name config opts]
  (let [opts (or opts #js {})
        nodes (parse-config config)
        base-dur (or (aget config "duration") 0)
        dur (Math/max base-dur (total-duration nodes))
        tl #js {:name name :nodes nodes :duration dur :baseDuration base-dur
                :time 0 :playing (not= (aget opts "autoplay") false)
                :zoom 1 :last nil :raf nil
                :loopOpt (let [l (aget opts "loop")] (if (= l js/undefined) false l))
                :subs #js [] :onChange (aget opts "onChange")}]
    (.push @timelines tl)
    (when (not= (aget opts "enabled") false)
      (build-dock! tl))
    (aset tl "raf" (js/requestAnimationFrame (fn [n] (aset tl "last" n) (tick! tl n))))
    (controller tl)))

(aset js/window "__uiDialTimeline" use-timeline)
(aset js/window "formatClock" fmt-clock)
