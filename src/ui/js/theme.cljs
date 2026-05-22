(ns theme)

;; ── Theme management ────────────────────────────────────────────────
;; Three modes: "auto" (system preference), "light", "dark".
;; Persists user choice to localStorage. On "auto", follows
;; prefers-color-scheme media query. Uses [data-no-transitions]
;; to suppress transition flash during switches.

(def ^:private storage-key "ui-theme")
(def ^:private modes #js ["auto" "light" "dark"])

(defn- get-stored []
  (try
    (.getItem js/localStorage storage-key)
    (catch :default _e nil)))

(defn- store! [mode]
  (try
    (if (= mode "auto")
      (.removeItem js/localStorage storage-key)
      (.setItem js/localStorage storage-key mode))
    (catch :default _e nil)))

(defn- system-prefers-dark? []
  (.-matches (.matchMedia js/window "(prefers-color-scheme: dark)")))

(defn- resolve-effective
  "Given a mode, return the effective theme: \"light\" or \"dark\"."
  [mode]
  (case mode
    "light" "light"
    "dark" "dark"
    (if (system-prefers-dark?) "dark" "light")))

(defn- suppress-transitions!
  "Temporarily disable transitions to prevent flash during theme switch."
  []
  (let [el js/document.documentElement]
    (.setAttribute el "data-no-transitions" "")
    ;; Force reflow so attribute takes effect before theme change
    (.-offsetHeight el)
    ;; Re-enable after a frame
    (js/requestAnimationFrame
     (fn []
       (js/requestAnimationFrame
        (fn []
          (.removeAttribute el "data-no-transitions")))))))

(defn- apply-theme!
  "Apply the given mode to the document element."
  [mode]
  (let [el js/document.documentElement]
    (suppress-transitions!)
    (case mode
      "light" (.setAttribute el "data-theme" "light")
      "dark" (.setAttribute el "data-theme" "dark")
      ;; auto — remove data-theme, let CSS media query decide
      (.removeAttribute el "data-theme"))))

;; ── Subscriber notifications ────────────────────────────────────────

(def ^:private subscribers (atom #js []))

(defn- notify! [mode effective]
  (let [subs @subscribers]
    (.forEach subs (fn [f] (f #js {:mode mode :effective effective})))))

;; ── Public API ──────────────────────────────────────────────────────

(defn get-mode
  "Return the current mode: \"auto\", \"light\", or \"dark\"."
  []
  (or (get-stored) "auto"))

(defn get-effective
  "Return the resolved theme: \"light\" or \"dark\"."
  []
  (resolve-effective (get-mode)))

(defn set-mode!
  "Set theme mode to \"auto\", \"light\", or \"dark\"."
  [mode]
  (let [m (if (#{"auto" "light" "dark"} mode) mode "auto")]
    (store! m)
    (apply-theme! m)
    (notify! m (resolve-effective m))))

(defn toggle!
  "Cycle through auto → light → dark → auto."
  []
  (let [current (get-mode)
        next-mode (case current
                    "auto" "light"
                    "light" "dark"
                    "dark" "auto"
                    "auto")]
    (set-mode! next-mode)
    next-mode))

(defn subscribe!
  "Register a callback for theme changes. Called with {mode, effective}.
   Returns an unsubscribe function."
  [f]
  (swap! subscribers (fn [subs] (.concat subs #js [f])))
  (fn [] (swap! subscribers (fn [subs] (.filter subs (fn [s] (not= s f)))))))

(defn init!
  "Initialize theme from stored preference or system default.
   Call once on page load."
  []
  (let [mode (get-mode)]
    (apply-theme! mode)
    ;; Listen for system preference changes (relevant when mode is auto)
    (let [mql (.matchMedia js/window "(prefers-color-scheme: dark)")]
      (.addEventListener mql "change"
                         (fn [_e]
                           (when (= (get-mode) "auto")
                             (apply-theme! "auto")
                             (notify! "auto" (resolve-effective "auto"))))))))

;; ── Attach to window ────────────────────────────────────────────────

(aset js/window "__uiTheme"
      #js {:init    init!
           :set     set-mode!
           :get     get-mode
           :effective get-effective
           :toggle  toggle!
           :subscribe subscribe!})
