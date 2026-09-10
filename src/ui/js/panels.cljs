(ns panels
  "Resizable panel groups runtime — a dependency-free port of the
   motion-panels core (https://github.com/letstri/motion-panels),
   with WAAPI animations instead of the motion library.

   Self-initializing: scans for [data-ui-panels-group] at load and on
   DOM changes. Works identically for hiccup (server HTML), replicant
   and squint targets — the .cljc component (ui.panels) only renders
   data-attributed markup, all behavior lives here.

   DOM contract (rendered by ui.panels):
     [data-ui-panels-group data-orientation=horizontal|vertical]
       [data-ui-panels-panel data-size=240|30% data-min-size data-max-size
        data-default-size data-collapsible data-collapsed data-persist-key]
         .ui-panels-content   (panel body)
         .ui-panels-edge      (grip for separator-less edge drag)
       [data-ui-panels-separator] > .ui-panels-grip
       [data-ui-panels-fill data-pin?] (> .ui-panels-fill-inner when pinned)

   Engine-managed attributes (styling hooks):
     panel: data-edge=start|end, data-bare, data-resizing, data-folding,
            data-collapsed
     grips: data-resizing, aria-value*

   Events (bubbling CustomEvents on the panel element):
     ui-panels-resize   detail {size pixels}  (size is px number or \"NN%\")
     ui-panels-collapse detail {collapsed}

   Controlled usage: toggle data-collapsed or set data-size from app
   code — an attribute observer picks it up and animates.

   Persistence: data-persist-key saves {size collapsed} to localStorage
   and restores it on init.

   Manual re-scan: window.__uiPanels()")

(def ^:private easing "cubic-bezier(0.32, 0.72, 0, 1)")
(def ^:private duration 250)
(def ^:private key-step 10)
(def ^:private key-step-fast 50)
(def ^:private pan-threshold 3)

(def ^:private axes-config
  #js {:horizontal #js {:client "clientWidth" :extent "width"
                        :cursor "col-resize" :grow "ArrowRight"
                        :shrink "ArrowLeft" :sepOrient "vertical"}
       :vertical   #js {:client "clientHeight" :extent "height"
                        :cursor "row-resize" :grow "ArrowDown"
                        :shrink "ArrowUp" :sepOrient "horizontal"}})

;; ── Small helpers ───────────────────────────────────────────────────

(defn- clamp [v lo hi]
  (js/Math.min (js/Math.max v lo) hi))

(defn- round2 [v]
  (/ (js/Math.round (* v 100)) 100))

(defn- reduced-motion? []
  (.-matches (js/matchMedia "(prefers-reduced-motion: reduce)")))

(defn- has-attr? [el attr]
  (and (some? el)
       (some? (.-hasAttribute el))
       (.hasAttribute el attr)))

(defn- fill-node? [el] (has-attr? el "data-ui-panels-fill"))
(defn- separator-node? [el] (has-attr? el "data-ui-panels-separator"))

(defn- fill-after?
  "True when a fill panel sits somewhere after `el` among its siblings."
  [el]
  (loop [n (.-nextElementSibling el)]
    (cond
      (nil? n) false
      (fill-node? n) true
      :else (recur (.-nextElementSibling n)))))

(defn- extent-key [group]
  (aget (.-axes group) "extent"))

(defn- group-extent [group]
  (aget (.-el group) (aget (.-axes group) "client")))

(defn- parse-size
  "\"240\" → {pct false value 240}; \"30%\" → {pct true value 30}; nil-safe."
  [s]
  (when (and (some? s) (not= s ""))
    (let [pct (.endsWith s "%")
          v (js/parseFloat s)]
      (when-not (js/isNaN v)
        #js {:pct pct :value v}))))

(defn- to-pixels [parsed total]
  (if (.-pct parsed)
    (* (/ (.-value parsed) 100) total)
    (.-value parsed)))

(defn- measure
  "The panel's logical size in pixels (percent sizes are rounded)."
  [panel]
  (let [total (group-extent (.-group panel))]
    (if (.-pct panel)
      (js/Math.round (* (/ (.-value panel) 100) total))
      (.-value panel))))

(defn- current-extent
  "The rendered extent right now (animation-aware)."
  [panel]
  (aget (.getBoundingClientRect (.-el panel)) (extent-key (.-group panel))))

(defn- size-report
  "What events report: px number, or a percent string for % panels."
  [panel]
  (if (.-pct panel)
    (str (.-value panel) "%")
    (.-value panel)))

;; ── Body lock during drags ──────────────────────────────────────────

(def ^:private lock-state #js {:count 0 :saved nil})

(defn- lock-body! [cursor on-escape]
  (let [style (.. js/document -body -style)
        onkey (fn [e]
                (when (= (.-key e) "Escape")
                  (.preventDefault e)
                  (on-escape)))]
    (when (zero? (.-count lock-state))
      (aset lock-state "saved"
            #js {:cursor (.-cursor style)
                 :userSelect (.-userSelect style)
                 :webkitUserSelect (.-webkitUserSelect style)}))
    (aset lock-state "count" (inc (.-count lock-state)))
    (set! (.-cursor style) cursor)
    (set! (.-userSelect style) "none")
    (set! (.-webkitUserSelect style) "none")
    (js/addEventListener "keydown" onkey true)
    (fn []
      (aset lock-state "count" (dec (.-count lock-state)))
      (when (zero? (.-count lock-state))
        (let [saved (.-saved lock-state)]
          (set! (.-cursor style) (.-cursor saved))
          (set! (.-userSelect style) (.-userSelect saved))
          (set! (.-webkitUserSelect style) (.-webkitUserSelect saved))))
      (js/removeEventListener "keydown" onkey true))))

;; ── Persistence ─────────────────────────────────────────────────────

(defn- persist! [panel]
  (when (.-persist panel)
    (try
      (js/localStorage.setItem
       (str "ui-panels:" (.-persist panel))
       (js/JSON.stringify #js {:size (size-report panel)
                               :collapsed (if (.-collapsed panel) true false)}))
      (catch :default _ nil))))

(defn- restore-persisted [key]
  (try
    (when-let [raw (js/localStorage.getItem (str "ui-panels:" key))]
      (js/JSON.parse raw))
    (catch :default _ nil)))

;; ── Events / attribute writes ───────────────────────────────────────

(defn- emit! [panel type detail]
  (.dispatchEvent (.-el panel)
                  (js/CustomEvent. type #js {:bubbles true :detail detail})))

(defn- emit-resize! [panel]
  (emit! panel "ui-panels-resize"
         #js {:size (size-report panel) :pixels (measure panel)}))

(defn- reflect-collapsed!
  "Mirror panel.collapsed onto the data-collapsed attribute. The
   attribute observer ignores mutations that already match state."
  [panel]
  (if (.-collapsed panel)
    (.setAttribute (.-el panel) "data-collapsed" "true")
    (.removeAttribute (.-el panel) "data-collapsed")))

;; ── Style writes ────────────────────────────────────────────────────

(defn- jump-size! [panel px]
  (aset (.-style (.-el panel)) (extent-key (.-group panel)) (str px "px")))

(defn- jump-content! [panel px]
  (when-let [content (.-content panel)]
    (aset (.-style content) (extent-key (.-group panel)) (str px "px"))))

;; ── Bounds ──────────────────────────────────────────────────────────

(defn- available
  "Room the panel may occupy: the fill's current extent plus every
   panel's overshoot past its target (self counts in full)."
  [panel]
  (let [group (.-group panel)
        k (extent-key group)
        fill (.-fill group)
        base (if (some? fill)
               (js/parseFloat (aget (js/getComputedStyle fill) k))
               0)]
    (loop [i 0 room base]
      (if (< i (.-length (.-panels group)))
        (let [p (aget (.-panels group) i)]
          (recur (inc i)
                 (+ room (- (current-extent p)
                            (if (identical? p panel) 0 (.-target p))))))
        (js/Math.max 0 room)))))

(defn- bounds [panel]
  (let [room (available panel)
        total (group-extent (.-group panel))
        minv (js/Math.min
              (if (some? (.-minSize panel)) (to-pixels (.-minSize panel) total) 0)
              room)
        maxv (if (some? (.-maxSize panel)) (to-pixels (.-maxSize panel) total) room)]
    #js {:min minv :max (js/Math.max minv (js/Math.min maxv room))}))

(defn- grow-sign [panel]
  (if (.-end panel) 1 -1))

;; ── ARIA ────────────────────────────────────────────────────────────

(defn- sync-aria! [panel]
  (let [b (bounds panel)
        t (js/Math.round (.-target panel))]
    (.forEach (.-grips panel)
              (fn [grip]
                (.setAttribute grip "aria-valuenow" (str t))
                (.setAttribute grip "aria-valuetext" (str t " pixels"))
                (.setAttribute grip "aria-valuemin" (str (js/Math.round (.-min b))))
                (if (some? (.-maxSize panel))
                  (.setAttribute grip "aria-valuemax" (str (js/Math.round (.-max b))))
                  (.removeAttribute grip "aria-valuemax"))))))

;; ── Folding (collapse animation + pinned fill) ──────────────────────

(defn- any-folding? [group]
  (.some (.-panels group) (fn [p] (if (.-folding p) true false))))

(defn- freeze-fill!
  "Pinned fill: anchor its inner content away from the folding panel
   and freeze the inner extent so the fill content doesn't reflow.
   Must run before the panel's style is moved to its new target (the
   available room is measured from the current layout)."
  [panel]
  (let [group (.-group panel)
        fill (.-fill group)
        inner (.-fillInner group)]
    (when (some? inner)
      (set! (.. fill -style -justifyContent)
            (if (.-end panel) "flex-end" "flex-start"))
      (set! (.. fill -style -overflow) "clip")
      (aset (.-style inner) (extent-key group)
            (str (js/Math.max 0 (- (available panel) (.-target panel))) "px")))))

(defn- set-folding! [panel folding]
  (when (not= (if (.-folding panel) true false) (if folding true false))
    (aset panel "folding" folding)
    (if folding
      (.setAttribute (.-el panel) "data-folding" "true")
      (.removeAttribute (.-el panel) "data-folding"))
    (when-not folding
      (let [group (.-group panel)
            fill (.-fill group)
            inner (.-fillInner group)]
        (when (and (some? inner) (not (any-folding? group)))
          (aset (.-style inner) (extent-key group) "100%")
          (set! (.. fill -style -overflow) ""))))))

(defn- fold!
  "Animate the panel's outer extent to `to` px (WAAPI)."
  [panel to]
  (let [el (.-el panel)
        k (extent-key (.-group panel))
        from (current-extent panel)]
    (set-folding! panel true)
    ;; Refresh on every fold — a fold retargeted mid-animation must
    ;; re-freeze for the new target (set-folding! no-ops when already
    ;; folding).
    (freeze-fill! panel)
    (when-let [anim (.-anim panel)] (.cancel anim))
    (jump-size! panel to)
    ;; Opening from closed: content snaps to final size (it was kept at
    ;; the measured size while collapsed). Open→open resize: animate the
    ;; content alongside so it tracks the reveal.
    (let [content-from (when (and (pos? to) (> from 0))
                         (when-let [c (.-content panel)]
                           (aget (.getBoundingClientRect c) k)))]
      (when (pos? to) (jump-content! panel to))
      (if (or (reduced-motion?) (= from to))
        (set-folding! panel false)
        (let [kf #js {}
              _ (aset kf k #js [(str from "px") (str to "px")])
              anim (.animate el kf #js {:duration duration :easing easing})]
          (aset panel "anim" anim)
          (when (and (some? content-from) (some? (.-content panel))
                     (not= content-from to))
            (let [ckf #js {}]
              (aset ckf k #js [(str content-from "px") (str to "px")])
              (.animate (.-content panel) ckf
                        #js {:duration duration :easing easing})))
          (set! (.-onfinish anim)
                (fn []
                  (aset panel "anim" nil)
                  (set-folding! panel false)))
          (set! (.-oncancel anim)
                (fn [] (aset panel "anim" nil))))))))

;; ── Target management ───────────────────────────────────────────────

(defn- retarget!
  "Recompute the target from logical size + collapsed state and move
   there (animated unless mid-drag)."
  [panel]
  (let [m (measure panel)
        value (if (.-collapsed panel) 0 m)]
    ;; Keep the hidden content at full size while closed so reopening
    ;; reveals it at its final dimensions.
    (when (and (zero? (current-extent panel)) (not (.-dragging panel)))
      (jump-content! panel m))
    (when (not= value (.-target panel))
      (aset panel "target" value)
      (when-not (.-dragging panel)
        (if (= (current-extent panel) value)
          (do (set-folding! panel false)
              (jump-size! panel value)
              (when (pos? value) (jump-content! panel m)))
          (fold! panel value))))
    (sync-aria! panel)))

(defn- set-collapsed!
  "State-changing collapse toggle (Enter key, attribute, drag)."
  [panel collapsed]
  (when (not= (if collapsed true false) (if (.-collapsed panel) true false))
    (aset panel "collapsed" collapsed)
    (reflect-collapsed! panel)
    (retarget! panel)
    (emit! panel "ui-panels-collapse" #js {:collapsed (if collapsed true false)})
    (persist! panel)))

(defn- apply-size!
  "Set the logical size from a pixel value (keyboard, reset, data-size)."
  [panel px]
  (let [b (bounds panel)
        v (clamp px (.-min b) (.-max b))
        total (group-extent (.-group panel))]
    (when (and (.-collapsed panel) (pos? v))
      (aset panel "collapsed" false)
      (reflect-collapsed! panel)
      (emit! panel "ui-panels-collapse" #js {:collapsed false}))
    (if (.-pct panel)
      (aset panel "value" (if (pos? total) (round2 (* 100 (/ v total))) 0))
      (aset panel "value" v))
    (retarget! panel)
    (emit-resize! panel)
    (persist! panel)))

;; ── Dragging ────────────────────────────────────────────────────────

(defn- release! [panel]
  (when-let [unlock (.-unlock panel)]
    (unlock)
    (aset panel "unlock" nil)))

(defn- set-resizing-attrs! [panel on]
  (let [f (fn [el]
            (if on
              (.setAttribute el "data-resizing" "true")
              (.removeAttribute el "data-resizing")))]
    (f (.-el panel))
    (.forEach (.-grips panel) f)))

(defn- stop-dragging! [panel]
  (release! panel)
  (aset panel "dragging" false)
  (set-resizing-attrs! panel false))

(defn- drag-collapse!
  "Collapse/expand mid-drag: no animation, just state + events."
  [panel collapsed]
  (aset panel "collapsed" collapsed)
  (reflect-collapsed! panel)
  (aset panel "target" (if collapsed 0 (measure panel)))
  (emit! panel "ui-panels-collapse" #js {:collapsed (if collapsed true false)}))

(defn- drag-cancel! [panel]
  (when (.-dragging panel)
    (let [s (.-session panel)]
      (jump-size! panel (.-start s))
      (jump-content! panel (if (pos? (.-start s)) (.-start s) (measure panel)))
      (when (not= (if (.-sessionCollapsed s) true false)
                  (if (.-wasCollapsed s) true false))
        (drag-collapse! panel (.-wasCollapsed s))))
    (stop-dragging! panel)))

(defn- drag-start! [panel cursor]
  (let [b (bounds panel)
        s (.-session panel)
        group (.-group panel)]
    ;; If an animation is in flight, freeze at the current value first
    ;; so cancelling it doesn't snap.
    (let [cur (current-extent panel)]
      (when-let [anim (.-anim panel)]
        (.cancel anim)
        (jump-size! panel cur))
      (aset s "min" (.-min b))
      (aset s "max" (.-max b))
      (aset s "sign" (grow-sign panel))
      (aset s "start" cur)
      (aset s "sessionCollapsed" (if (.-collapsed panel) true false))
      (aset s "wasCollapsed" (if (.-collapsed panel) true false)))
    (release! panel)
    (aset panel "unlock"
          (lock-body! (if (some? cursor) cursor (aget (.-axes group) "cursor"))
                      (fn [] (drag-cancel! panel))))
    (set-folding! panel false)
    (aset panel "dragging" true)
    (set-resizing-attrs! panel true)))

(defn- drag-move! [panel dx dy]
  (when (.-dragging panel)
    (let [s (.-session panel)
          group (.-group panel)
          offset (if (= (extent-key group) "width") dx dy)
          pixels (+ (.-start s) (* offset (.-sign s)))
          collapse (and (.-collapsible panel)
                        (< pixels (/ (.-min s) 2)))
          next (if collapse
                 0
                 (js/Math.round (clamp pixels (.-min s) (.-max s))))]
      (when (not= (if collapse true false)
                  (if (.-sessionCollapsed s) true false))
        (drag-collapse! panel collapse))
      (jump-size! panel next)
      (when-not collapse (jump-content! panel next))
      (aset s "sessionCollapsed" collapse))))

(defn- drag-end! [panel]
  (when (.-dragging panel)
    (let [s (.-session panel)]
      (stop-dragging! panel)
      (if (.-sessionCollapsed s)
        (persist! panel)
        (let [px (current-extent panel)
              total (group-extent (.-group panel))]
          (if (.-pct panel)
            (aset panel "value" (if (pos? total) (round2 (* 100 (/ px total))) 0))
            (aset panel "value" px))
          (aset panel "target" (measure panel))
          (emit-resize! panel)
          (persist! panel)))
      (let [cur (current-extent panel)]
        (when (not= cur (.-target panel))
          (fold! panel (.-target panel))))
      (sync-aria! panel))))

;; ── Keyboard / reset ────────────────────────────────────────────────

(defn- resize-by-key! [panel e]
  (let [key (.-key e)
        axes (.-axes (.-group panel))]
    (if (= key "Enter")
      (when (.-collapsible panel)
        (.preventDefault e)
        (set-collapsed! panel (not (.-collapsed panel))))
      (let [b (bounds panel)
            fast (or (.-shiftKey e) (= key "PageUp") (= key "PageDown"))
            step (* (if fast key-step-fast key-step) (grow-sign panel))
            t (.-target panel)
            next (cond
                   (= key "End") (.-max b)
                   (= key "Home") (.-min b)
                   (= key "PageDown") (+ t step)
                   (= key "PageUp") (- t step)
                   (= key (aget axes "grow")) (+ t step)
                   (= key (aget axes "shrink")) (- t step)
                   :else nil)]
        (when (some? next)
          (.preventDefault e)
          (apply-size! panel (clamp next (.-min b) (.-max b))))))))

(defn- reset-size! [panel]
  (when (.-collapsed panel)
    (set-collapsed! panel false))
  (let [total (group-extent (.-group panel))
        parsed (if (some? (.-defaultSize panel))
                 (.-defaultSize panel)
                 (.-initial panel))]
    (when (some? parsed)
      (apply-size! panel (to-pixels parsed total)))))

;; ── Grips (shared by separators and panel edges) ────────────────────

(defn- attach-grip!
  "Wire pointer/keyboard/dblclick handling onto a grip element.
   `get-panel` resolves the controlled panel at interaction time (a
   separator's panel can change when children are reordered)."
  [grip get-panel]
  (let [state #js {:pressed nil :dragging false :dragged false}
        settle (fn []
                 (aset state "pressed" nil)
                 (aset state "dragging" false))]
    (.addEventListener grip "pointerdown"
                       (fn [e]
                         (when (some? (get-panel))
                           (aset state "dragged" false)
                           (aset state "pressed"
                                 #js {:x (.-clientX e) :y (.-clientY e)})
                           (.setPointerCapture grip (.-pointerId e)))))
    (.addEventListener grip "pointermove"
                       (fn [e]
                         (when-let [pr (.-pressed state)]
                           (when-let [p (get-panel)]
                             (let [dx (- (.-clientX e) (.-x pr))
                                   dy (- (.-clientY e) (.-y pr))]
                               (when (and (not (.-dragging state))
                                          (>= (js/Math.hypot dx dy) pan-threshold))
                                 (aset state "dragging" true)
                                 (aset state "dragged" true)
                                 (drag-start! p nil))
                               (when (.-dragging state)
                                 (drag-move! p dx dy)))))))
    (.addEventListener grip "pointerup"
                       (fn [_]
                         (when (.-dragging state)
                           (when-let [p (get-panel)] (drag-end! p)))
                         (settle)))
    (.addEventListener grip "pointercancel"
                       (fn [_]
                         (when (.-dragging state)
                           (when-let [p (get-panel)] (drag-cancel! p)))
                         (settle)))
    (.addEventListener grip "dblclick"
                       (fn [_]
                         (when-not (.-dragged state)
                           (when-let [p (get-panel)] (reset-size! p)))))
    (.addEventListener grip "keydown"
                       (fn [e]
                         (when-let [p (get-panel)] (resize-by-key! p e))))))

(defn- separator-panel
  "The sized panel a separator controls: the one on the fill's start
   side when the fill comes after the separator, else the end side."
  [group slot]
  (aget (.-bySide group) (if (fill-after? slot) "start" "end")))

;; ── Placement ───────────────────────────────────────────────────────

(defn- place-all!
  "(Re)derive placement from the DOM: which side of the fill each panel
   sits on, bare edges, separator→panel bindings, pin wiring. Runs on
   init and whenever the group's children change (reorder support)."
  [group]
  (let [gel (.-el group)
        children (js/Array.from (.-children gel))
        fill (.find children (fn [c] (fill-node? c)))
        by-side #js {}]
    (aset group "fill" (if (some? fill) fill nil))
    (aset group "fillInner"
          (if (and (some? fill) (.hasAttribute fill "data-pin"))
            (.querySelector fill ":scope > .ui-panels-fill-inner")
            nil))
    ;; Panels: side, bare edge, grip list (own edge first).
    (.forEach (.-panels group)
              (fn [p]
                (let [el (.-el p)
                      end (fill-after? el)
                      neighbour (if end
                                  (.-nextElementSibling el)
                                  (.-previousElementSibling el))
                      bare (not (separator-node? neighbour))]
                  (aset p "end" end)
                  (.setAttribute el "data-edge" (if end "end" "start"))
                  (if bare
                    (.setAttribute el "data-bare" "true")
                    (.removeAttribute el "data-bare"))
                  (aset by-side (if end "start" "end") p)
                  (aset p "grips"
                        (if (some? (.-edge p)) #js [(.-edge p)] #js [])))))
    (aset group "bySide" by-side)
    ;; Separators: register their grip with the panel they control.
    (.forEach children
              (fn [c]
                (when (separator-node? c)
                  (when-let [grip (aget c "__uiSepGrip")]
                    (when-let [p (separator-panel group c)]
                      (.push (.-grips p) grip))))))
    (.forEach (.-panels group) (fn [p] (sync-aria! p)))))

;; ── Panel init ──────────────────────────────────────────────────────

(defn- observe-panel-attrs!
  "Controlled usage: react to external data-collapsed / data-size
   changes. Engine-originated writes already match state and no-op."
  [panel]
  (let [el (.-el panel)
        mo (js/MutationObserver.
            (fn [muts]
              (.forEach muts
                        (fn [m]
                          (let [attr (.-attributeName m)]
                            (cond
                              (= attr "data-collapsed")
                              (let [want (.hasAttribute el "data-collapsed")]
                                (when (not= (if want true false)
                                            (if (.-collapsed panel) true false))
                                  (set-collapsed! panel want)))

                              (= attr "data-size")
                              (when-let [parsed (parse-size (.getAttribute el "data-size"))]
                                (when (or (not= (.-value parsed) (.-value panel))
                                          (not= (if (.-pct parsed) true false)
                                                (if (.-pct panel) true false)))
                                  (aset panel "pct" (.-pct parsed))
                                  (aset panel "value" (.-value parsed))
                                  (retarget! panel)))))))))]
    (.observe mo el #js {:attributes true
                         :attributeFilter #js ["data-collapsed" "data-size"]})
    (aset panel "attrObserver" mo)))

(defn- create-panel! [group el]
  (let [content (.querySelector el ":scope > .ui-panels-content")
        edge (.querySelector el ":scope > .ui-panels-edge")
        parsed (parse-size (.getAttribute el "data-size"))
        persist-key (.getAttribute el "data-persist-key")
        saved (when (some? persist-key) (restore-persisted persist-key))
        saved-size (when (some? saved) (parse-size (str (.-size saved))))
        active (if (some? saved-size) saved-size parsed)
        collapsed (if (some? saved)
                    (if (.-collapsed saved) true false)
                    (.hasAttribute el "data-collapsed"))
        panel #js {:el el :content content :edge edge :group group
                   :pct (if (some? active) (.-pct active) false)
                   :value (if (some? active) (.-value active) 0)
                   :initial parsed
                   :minSize (parse-size (.getAttribute el "data-min-size"))
                   :maxSize (parse-size (.getAttribute el "data-max-size"))
                   :defaultSize (parse-size (.getAttribute el "data-default-size"))
                   :collapsible (.hasAttribute el "data-collapsible")
                   :collapsed collapsed
                   :persist persist-key
                   :target 0 :end false :dragging false :folding false
                   :session #js {} :anim nil :unlock nil
                   :grips #js []}]
    (aset el "__uiPanel" panel)
    (.push (.-panels group) panel)
    ;; First paint: jump straight to the resting state, no animation.
    (let [m (measure panel)
          t (if collapsed 0 m)]
      (aset panel "target" t)
      (reflect-collapsed! panel)
      (jump-size! panel t)
      (jump-content! panel m))
    (when (some? edge)
      (.setAttribute edge "aria-orientation" (aget (.-axes group) "sepOrient"))
      (attach-grip! edge (fn [] panel)))
    (observe-panel-attrs! panel)
    panel))

(defn- attach-separator! [group slot]
  (let [grip (.querySelector slot ":scope > .ui-panels-grip")
        grip (if (some? grip) grip slot)]
    (aset slot "__uiSepGrip" grip)
    (.setAttribute grip "aria-orientation" (aget (.-axes group) "sepOrient"))
    (attach-grip! grip (fn [] (separator-panel group slot)))))

;; ── Group init ──────────────────────────────────────────────────────

(defn- bind-children!
  "Create controllers for any unbound panel/separator children."
  [group]
  (.forEach (js/Array.from (.-children (.-el group)))
            (fn [c]
              (cond
                (and (has-attr? c "data-ui-panels-panel")
                     (nil? (aget c "__uiPanel")))
                (create-panel! group c)

                (and (separator-node? c)
                     (nil? (aget c "__uiSepGrip")))
                (attach-separator! group c)))))

(defn- prune-panels!
  "Drop controllers whose elements left the group (reorder/removal)."
  [group]
  (let [gel (.-el group)
        kept (.filter (.-panels group)
                      (fn [p] (identical? (.-parentElement (.-el p)) gel)))]
    (.forEach (.-panels group)
              (fn [p]
                (when-not (identical? (.-parentElement (.-el p)) gel)
                  (release! p)
                  (when-let [mo (.-attrObserver p)] (.disconnect mo)))))
    (aset group "panels" kept)))

(defn- refit!
  "Percent panels track the group as it resizes."
  [panel]
  (when (and (.-pct panel)
             (not (.-dragging panel))
             (not (.-folding panel)))
    (let [m (measure panel)
          v (if (.-collapsed panel) 0 m)]
      (jump-content! panel m)
      (when (not= v (.-target panel))
        (aset panel "target" v)
        (jump-size! panel v)
        (sync-aria! panel)))))

(defn- init-group! [gel]
  (when (nil? (aget gel "__uiPanelsGroup"))
    (let [orientation (let [o (.getAttribute gel "data-orientation")]
                        (if (= o "vertical") "vertical" "horizontal"))
          group #js {:el gel
                     :axes (aget axes-config orientation)
                     :panels #js []
                     :bySide #js {}
                     :fill nil
                     :fillInner nil}]
      (aset gel "__uiPanelsGroup" group)
      (bind-children! group)
      (place-all! group)
      ;; Reorder / dynamic children: rebind + re-place (no animation).
      (let [mo (js/MutationObserver.
                (fn [_]
                  (prune-panels! group)
                  (bind-children! group)
                  (place-all! group)))]
        (.observe mo gel #js {:childList true}))
      ;; Group resize: refit percent panels.
      (let [ro (js/ResizeObserver.
                (fn [_]
                  (.forEach (.-panels group) (fn [p] (refit! p)))))]
        (.observe ro gel))
      group)))

;; ── Bootstrap ───────────────────────────────────────────────────────

(defn- scan! []
  (.forEach (js/Array.from
             (js/document.querySelectorAll "[data-ui-panels-group]"))
            (fn [gel] (init-group! gel))))

(def ^:private scan-scheduled (atom nil))

(defn- schedule-scan! []
  (when (nil? @scan-scheduled)
    (reset! scan-scheduled
            (js/requestAnimationFrame
             (fn []
               (reset! scan-scheduled nil)
               (scan!))))))

(aset js/window "__uiPanels" scan!)

(defn- start! []
  (scan!)
  (let [mo (js/MutationObserver. (fn [_] (schedule-scan!)))]
    (.observe mo js/document.body #js {:childList true :subtree true})))

(if (= js/document.readyState "loading")
  (js/document.addEventListener "DOMContentLoaded" (fn [] (start!)))
  (start!))
