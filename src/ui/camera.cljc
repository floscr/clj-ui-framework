(ns ui.camera
  "Camera capture: capability detection, getUserMedia stream helpers, and
   a viewfinder component with a native <input capture> fallback.

   Detection (browser targets only):
   - camera-capable?  - HTTP-safe touch/coarse-pointer heuristic
   - has-camera-api?  - getUserMedia availability (needs HTTPS/localhost)
   - detect-camera!   - refine via enumerateDevices() in secure contexts

   Stream helpers (browser targets only) manage a single module-level
   stream: start-camera!, stop-camera!, capture-photo!.

   Markup:
   - camera-view - live viewfinder (crosshair + capture/switch buttons)
     when :active?, otherwise a tap-to-capture placeholder wrapping a
     native <input type=file capture> (launches the OS camera on mobile
     and works over plain HTTP, where getUserMedia is unavailable)."
  (:require [ui.icon :as icon]
            [ui.util :as util]))

;; ── Detection + stream helpers (browser targets only) ───────────────

(do
  #?@(:squint
      [(defn camera-capable?
         "Best-effort, HTTP-safe guess for devices whose file-input
          `capture` attribute actually launches a camera — phones and
          tablets. Uses touch + coarse-pointer media queries, which work
          over plain HTTP (unlike navigator.mediaDevices). Use as the
          initial seed; refine with detect-camera! in secure contexts."
         []
         (boolean
          (and (pos? (or (.-maxTouchPoints js/navigator) 0))
               (.-matchMedia js/window)
               (.-matches (js/matchMedia "(pointer: coarse)")))))

       (defn has-camera-api?
         "True when getUserMedia is available (requires a secure
          context: HTTPS or localhost)."
         []
         (boolean
          (and (.-mediaDevices js/navigator)
               (.-getUserMedia (.-mediaDevices js/navigator)))))

       (defn detect-camera!
         "Refine camera capability via enumerateDevices(). In a secure
          context, calls (callback capable?) with whether any videoinput
          device exists (no permission prompt). Over plain HTTP the API
          is unavailable and callback is never called — keep the
          camera-capable? heuristic."
         [callback]
         (let [md (.-mediaDevices js/navigator)]
           (when (and (.-isSecureContext js/window) md (.-enumerateDevices md))
             (-> (.enumerateDevices md)
                 (.then (fn [devices]
                          (callback
                           (boolean
                            (some (fn [d] (= "videoinput" (.-kind d)))
                                  (js/Array.from devices))))))
                 (.catch (fn [_] nil))))))

       (defonce ^:private stream* (atom nil))

       (defn stop-camera!
         "Stop the active camera stream, if any."
         []
         (when-let [stream @stream*]
           (doseq [track (.getTracks stream)]
             (.stop track))
           (reset! stream* nil)))

       (defn start-camera!
         "Start a getUserMedia stream and attach it to the <video>
          element rendered by camera-view. Stops any previous stream.

          Opts:
            :video-id  - id of the <video> element (default \"ui-camera-video\")
            :facing    - \"environment\" (default) | \"user\"
            :on-active - (fn []) called when the stream starts
            :on-error  - (fn [err]) getUserMedia unavailable or failed"
         [{:keys [video-id facing on-active on-error]}]
         (stop-camera!)
         (if-not (has-camera-api?)
           (when on-error
             (on-error (js/Error. "getUserMedia not available (needs HTTPS)")))
           (-> (.getUserMedia (.-mediaDevices js/navigator)
                              {:video {:facingMode (or facing "environment")
                                       :width {:ideal 1920}
                                       :height {:ideal 1080}}})
               (.then (fn [stream]
                        (reset! stream* stream)
                        ;; on-active first: the consumer typically flips
                        ;; state that renders the <video>; the rAF then
                        ;; finds it and attaches the stream.
                        (when on-active (on-active))
                        (js/requestAnimationFrame
                         (fn []
                           (when-let [video (js/document.getElementById
                                             (or video-id "ui-camera-video"))]
                             (set! (.-srcObject video) stream)
                             (.play video))))))
               (.catch (fn [err]
                         (when on-error (on-error err)))))))

       (defn capture-photo!
         "Capture the current viewfinder frame as a JPEG. Returns
          {:preview data-url :data base64 :mime-type \"image/jpeg\"} or
          nil when the video element is missing. Does not stop the
          stream — call stop-camera! separately.

          Opts: :video-id (default \"ui-camera-video\"),
                :quality (0-1, default 0.92)"
         [{:keys [video-id quality]}]
         (when-let [video (js/document.getElementById
                           (or video-id "ui-camera-video"))]
           (let [canvas (js/document.createElement "canvas")
                 w (.-videoWidth video)
                 h (.-videoHeight video)]
             (set! (.-width canvas) w)
             (set! (.-height canvas) h)
             (.drawImage (.getContext canvas "2d") video 0 0 w h)
             (let [data-url (.toDataURL canvas "image/jpeg" (or quality 0.92))
                   idx (.indexOf data-url ",")]
               {:preview data-url
                :data (when (pos? idx) (.substring data-url (inc idx)))
                :mime-type "image/jpeg"}))))]

      :cljs
      [(defn camera-capable?
         "Best-effort, HTTP-safe guess for devices whose file-input
          `capture` attribute actually launches a camera — phones and
          tablets. Uses touch + coarse-pointer media queries, which work
          over plain HTTP (unlike navigator.mediaDevices). Use as the
          initial seed; refine with detect-camera! in secure contexts."
         []
         (boolean
          (and (pos? (or (.-maxTouchPoints js/navigator) 0))
               (.-matchMedia js/window)
               (.-matches (js/matchMedia "(pointer: coarse)")))))

       (defn has-camera-api?
         "True when getUserMedia is available (requires a secure
          context: HTTPS or localhost)."
         []
         (boolean
          (and (.-mediaDevices js/navigator)
               (.-getUserMedia (.-mediaDevices js/navigator)))))

       (defn detect-camera!
         "Refine camera capability via enumerateDevices(). In a secure
          context, calls (callback capable?) with whether any videoinput
          device exists (no permission prompt). Over plain HTTP the API
          is unavailable and callback is never called — keep the
          camera-capable? heuristic."
         [callback]
         (let [md (.-mediaDevices js/navigator)]
           (when (and (.-isSecureContext js/window) md (.-enumerateDevices md))
             (-> (.enumerateDevices md)
                 (.then (fn [devices]
                          (callback
                           (boolean
                            (some (fn [d] (= "videoinput" (.-kind d)))
                                  (js/Array.from devices))))))
                 (.catch (fn [_] nil))))))

       (defonce ^:private stream* (atom nil))

       (defn stop-camera!
         "Stop the active camera stream, if any."
         []
         (when-let [stream @stream*]
           (doseq [track (.getTracks stream)]
             (.stop track))
           (reset! stream* nil)))

       (defn start-camera!
         "Start a getUserMedia stream and attach it to the <video>
          element rendered by camera-view. Stops any previous stream.

          Opts:
            :video-id  - id of the <video> element (default \"ui-camera-video\")
            :facing    - \"environment\" (default) | \"user\"
            :on-active - (fn []) called when the stream starts
            :on-error  - (fn [err]) getUserMedia unavailable or failed"
         [{:keys [video-id facing on-active on-error]}]
         (stop-camera!)
         (if-not (has-camera-api?)
           (when on-error
             (on-error (js/Error. "getUserMedia not available (needs HTTPS)")))
           (-> (.getUserMedia (.-mediaDevices js/navigator)
                              (clj->js {:video {:facingMode (or facing "environment")
                                                :width {:ideal 1920}
                                                :height {:ideal 1080}}}))
               (.then (fn [stream]
                        (reset! stream* stream)
                        ;; on-active first: the consumer typically flips
                        ;; state that renders the <video>; the rAF then
                        ;; finds it and attaches the stream.
                        (when on-active (on-active))
                        (js/requestAnimationFrame
                         (fn []
                           (when-let [video (js/document.getElementById
                                             (or video-id "ui-camera-video"))]
                             (set! (.-srcObject video) stream)
                             (.play video))))))
               (.catch (fn [err]
                         (when on-error (on-error err)))))))

       (defn capture-photo!
         "Capture the current viewfinder frame as a JPEG. Returns
          {:preview data-url :data base64 :mime-type \"image/jpeg\"} or
          nil when the video element is missing. Does not stop the
          stream — call stop-camera! separately.

          Opts: :video-id (default \"ui-camera-video\"),
                :quality (0-1, default 0.92)"
         [{:keys [video-id quality]}]
         (when-let [video (js/document.getElementById
                           (or video-id "ui-camera-video"))]
           (let [canvas (js/document.createElement "canvas")
                 w (.-videoWidth video)
                 h (.-videoHeight video)]
             (set! (.-width canvas) w)
             (set! (.-height canvas) h)
             (.drawImage (.getContext canvas "2d") video 0 0 w h)
             (let [data-url (.toDataURL canvas "image/jpeg" (or quality 0.92))
                   idx (.indexOf data-url ",")]
               {:preview data-url
                :data (when (pos? idx) (.substring data-url (inc idx)))
                :mime-type "image/jpeg"}))))]))

;; ── Components ──────────────────────────────────────────────────────

#?(:squint (defn- ->files [fs] (js/Array.from fs))
   :cljs   (defn- ->files [fs] (vec (array-seq fs)))
   :clj    (defn- ->files [fs] fs))

(defn camera-view
  "Camera capture area.

   When :active? — live viewfinder: <video> + overlay with a crosshair,
   a round capture button, and (when :on-switch is given) a facing
   toggle. Call start-camera! after rendering to attach the stream.

   When inactive — a tap-to-capture placeholder wrapping a native
   <input type=file capture> that launches the OS camera on mobile and
   works over plain HTTP.

   Props:
     :active?     - live viewfinder vs placeholder (default false)
     :video-id    - id for the <video> element (default \"ui-camera-video\")
     :facing      - \"environment\" (default) | \"user\" — used for the
                    native input's capture attribute
     :placeholder - placeholder label text (default \"Tap to take a photo\")
     :accept      - file input accept string (default \"image/*\")
     :on-capture  - (fn []) capture button clicked (:cljs/:squint)
     :on-switch   - (fn []) switch button clicked; button hidden when nil
     :on-files    - (fn [files]) native input selection (:cljs/:squint)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes map"
  [{:keys [active? video-id facing placeholder accept
           on-capture on-switch on-files class attrs] :as _props}]
  (let [vid (or video-id "ui-camera-video")
        facing (or facing "environment")
        ph (or placeholder "Tap to take a photo")
        accept (or accept "image/*")]
    #?(:squint
       [:div (merge {:class (cond-> "camera-section"
                              class (str " " class))}
                    attrs)
        (if active?
          [:div
           [:video {:id vid :autoplay true :playsinline true :muted true
                    :class "camera-video"}]
           [:div {:class "camera-overlay"}
            [:div {:class "camera-crosshair"}]
            [:button {:type "button" :class "camera-capture-btn"
                      :aria-label "Capture photo"
                      :on-click (fn [_] (when on-capture (on-capture)))}]
            (when on-switch
              [:button {:type "button" :class "camera-switch-btn"
                        :aria-label "Switch camera"
                        :on-click (fn [_] (on-switch))}
               (icon/icon {:icon-name :refresh :size :sm})])]]
          [:label {:class "camera-placeholder"}
           [:input {:type "file" :accept accept :capture facing
                    :class "sr-only"
                    :on-change (fn [e]
                                 (when on-files
                                   (on-files (->files (.. e -target -files)))))}]
           (icon/icon {:icon-name :camera :size :xl})
           [:p ph]])]

       :cljs
       [:div (merge {:class (cond-> ["camera-section"]
                              class (util/conj-classes class))}
                    attrs)
        (if active?
          [:div
           [:video {:id vid :autoPlay true :playsInline true :muted true
                    :class ["camera-video"]}]
           [:div {:class ["camera-overlay"]}
            [:div {:class ["camera-crosshair"]}]
            [:button {:type "button" :class ["camera-capture-btn"]
                      :aria-label "Capture photo"
                      :on {:click (fn [_] (when on-capture (on-capture)))}}]
            (when on-switch
              [:button {:type "button" :class ["camera-switch-btn"]
                        :aria-label "Switch camera"
                        :on {:click (fn [_] (on-switch))}}
               (icon/icon {:icon-name :refresh :size :sm})])]]
          [:label {:class ["camera-placeholder"]}
           [:input {:type "file" :accept accept :capture facing
                    :class ["sr-only"]
                    :on {:change (fn [e]
                                   (when on-files
                                     (on-files (->files (.. e -target -files)))))}}]
           (icon/icon {:icon-name :camera :size :xl})
           [:p ph]])]

       :clj
       [:div (merge {:class (cond-> "camera-section"
                              class (str " " class))}
                    attrs)
        (if active?
          [:div
           [:video {:id vid :autoplay true :playsinline true :muted true
                    :class "camera-video"}]
           [:div {:class "camera-overlay"}
            [:div {:class "camera-crosshair"}]
            [:button {:type "button" :class "camera-capture-btn"
                      :aria-label "Capture photo"}]
            (when on-switch
              [:button {:type "button" :class "camera-switch-btn"
                        :aria-label "Switch camera"}
               (icon/icon {:icon-name :refresh :size :sm})])]]
          [:label {:class "camera-placeholder"}
           [:input {:type "file" :accept accept :capture facing
                    :class "sr-only"}]
           (icon/icon {:icon-name :camera :size :xl})
           [:p ph]])])))
