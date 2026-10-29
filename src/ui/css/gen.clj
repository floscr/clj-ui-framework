(ns ui.css.gen
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ui.theme-model :as theme-model]))

(defn read-tokens
  "Read and parse the tokens EDN file."
  [path]
  (edn/read-string (slurp path)))

(defn token->css-var
  "Convert a token keyword to a CSS variable name."
  [k]
  (str "--" (name k)))

(defn tokens->css-block
  "Generate CSS variable declarations from a token map."
  [tokens]
  (->> tokens
       (sort-by key)
       (map (fn [[k v]] (str "  " (token->css-var k) ": " v ";")))
       (str/join "\n")))

(defn themable
  "`tokens` of `mode` (\"light\" / \"dark\") with each of
   `ui.theme-model/themable-tokens` read through --theme-<token>-<mode>
   first: the per-mode properties a parametric theme sets on <html>. A
   border keeps its width and style, only its color is themable."
  [tokens mode]
  (let [themable? (set (map keyword theme-model/themable-tokens))]
    (into {}
          (map (fn [[k v]]
                 [k (if-not (themable? k)
                      v
                      (let [hook (str "--theme-" (name k) "-" mode)]
                        (if-let [[_ line color] (re-matches #"(\d+px solid )(.+)" v)]
                          (str line "var(" hook ", " color ")")
                          (str "var(" hook ", " v ")"))))]))
          tokens)))

(defn format-number
  "Format a number: strip trailing zeros, max 3 decimal places."
  [n]
  (let [s (format "%.3f" (double n))]
    (-> s
        (str/replace #"0+$" "")
        (str/replace #"\.$" ""))))

;; ── OKLCH ────────────────────────────────────────────────────────

(defn oklch->srgb
  "Convert OKLCH [L C H] to sRGB [r g b] (0-255 clamped).
   L: 0-1, C: 0-~0.4, H: 0-360 degrees."
  [[l c h]]
  (let [h-rad (* h (/ Math/PI 180))
        ;; OKLCH → OKLab
        a (* c (Math/cos h-rad))
        b (* c (Math/sin h-rad))
        ;; OKLab → LMS (cube roots)
        l_ (+ l (* 0.3963377774 a) (* 0.2158037573 b))
        m_ (+ l (* -0.1055613458 a) (* -0.0638541728 b))
        s_ (+ l (* -0.0894841775 a) (* -1.2914855480 b))
        ;; Cube to get LMS
        l3 (* l_ l_ l_)
        m3 (* m_ m_ m_)
        s3 (* s_ s_ s_)
        ;; LMS → linear sRGB
        r-lin (+ (* 4.0767416621 l3) (* -3.3077115913 m3) (* 0.2309699292 s3))
        g-lin (+ (* -1.2684380046 l3) (* 2.6097574011 m3) (* -0.3413193965 s3))
        b-lin (+ (* -0.0041960863 l3) (* -0.7034186147 m3) (* 1.7076147010 s3))
        ;; Linear sRGB → sRGB (gamma)
        gamma (fn [x]
                (if (<= x 0.0031308)
                  (* 12.92 x)
                  (- (* 1.055 (Math/pow (max 0.0 x) (/ 1.0 2.4))) 0.055)))
        clamp (fn [x] (max 0 (min 255 (int (Math/round (* 255.0 (gamma (max 0.0 x))))))))]
    [(clamp r-lin) (clamp g-lin) (clamp b-lin)]))

(defn rgb->hex
  "Convert [r g b] (0-255 ints) to a hex color string."
  [[r g b]]
  (format "#%02x%02x%02x" r g b))

(defn oklch->hex
  "Convert OKLCH [L C H] to hex string. Clamps to sRGB gamut."
  [[l c h]]
  (rgb->hex (oklch->srgb [l c h])))

;; ── Scale generation ─────────────────────────────────────────────

(defn generate-size-scale
  "Generate linear size scale: --size-N = base * N."
  [{:keys [base unit steps]}]
  (->> (range 1 (inc steps))
       (map (fn [n]
              (str "  --size-" n ": " (format-number (* base n)) unit ";")))
       (str/join "\n")))

(defn generate-font-scale
  "Generate the font scale. Each step is either [label multiplier] (a string
   first: --font-{label} = base * multiplier, a hand-tuned ramp) or
   [power label] (a number first: --font-{label} = base * ratio^power, a
   geometric ramp)."
  [{:keys [base unit ratio steps]}]
  (->> steps
       (map (fn [[a b]]
              (let [[label value] (if (string? a)
                                    [a (* base b)]
                                    [b (* base (Math/pow ratio a))])]
                (str "  --font-" label ": " (format-number value) unit ";"))))
       (str/join "\n")))

(defn generate-color-scale
  "Generate CSS variables for a named OKLCH color scale.
   Each step is [label lightness] or [label lightness chroma].
   Outputs oklch() CSS values for perceptual uniformity.
   Optional :chroma-scale multiplier (default 1.0) scales all chroma values."
  [scale-name {:keys [hue chroma steps chroma-scale]}]
  (let [cs (or chroma-scale 1.0)]
    (->> steps
         (map (fn [step]
                (let [[label lightness chr] (if (= 3 (count step))
                                              step
                                              [(first step) (second step) chroma])
                      chr (min 0.4 (* (double chr) (double cs)))
                      ;; Format: oklch(L C H)
                      css-val (str "oklch(" (format "%.3f" (double lightness))
                                  " " (format "%.4f" chr)
                                  " " (format "%.1f" (double hue)) ")")]
                  (str "  --" (name scale-name) "-" label ": " css-val ";"))))
         (str/join "\n"))))

(defn generate-color-scales
  "Generate all color scale CSS variables."
  [color-scales]
  (->> color-scales
       (sort-by key)
       (map (fn [[scale-name config]]
              (generate-color-scale scale-name config)))
       (str/join "\n")))

(defn generate-scales
  "Generate CSS variable declarations for all scales."
  [scales]
  (str/join "\n"
    (cond-> []
      (:size scales)  (conj (generate-size-scale (:size scales)))
      (:font scales)  (conj (generate-font-scale (:font scales)))
      (:color scales) (conj (generate-color-scales (:color scales))))))

;; ── Accents ──────────────────────────────────────────────────────

(defn- indent-block
  "Indent every line of a CSS declaration block by two more spaces."
  [block]
  (str/replace block #"(?m)^  " "    "))

(defn generate-accent
  "CSS for one [data-accent=\"name\"] variant.
   {:scale :blue}            — remaps --accent-<step> onto the named scale
   {:tokens {..} :dark {..}} — overrides semantic tokens (light + dark)."
  [accent-name {:keys [scale tokens dark]} color-scales]
  (let [sel (str "[data-accent=\"" (name accent-name) "\"]")]
    (if scale
      (let [steps (map first (get-in color-scales [scale :steps]))]
        (str sel " {\n"
             (str/join "\n" (map #(str "  --accent-" % ": var(--" (name scale) "-" % ");") steps))
             "\n}"))
      (str sel " {\n" (tokens->css-block tokens) "\n}\n\n"
           sel "[data-theme=\"dark\"] {\n" (tokens->css-block dark) "\n}\n\n"
           "@media (prefers-color-scheme: dark) {\n"
           "  " sel ":not([data-theme=\"light\"]) {\n"
           (indent-block (tokens->css-block dark))
           "\n  }\n}"))))

(defn generate-accents
  "CSS blocks for every entry in :accents."
  [accents color-scales]
  (->> accents
       (sort-by key)
       (map (fn [[k v]] (generate-accent k v color-scales)))
       (str/join "\n\n")))

(defn base-css
  "Generate base body/reset styles: type, focus ring, reduced motion."
  []
  "*, *::before, *::after {
  box-sizing: border-box;
}

html {
  font-family: var(--font-body);
  line-height: var(--leading-body);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  margin: 0;
  background: var(--bg-0);
  color: var(--fg-0);
  font-size: var(--font-base);
}

/* Two weights for UI chrome: regular (400) and medium (500). Hierarchy
   comes from size, color and spacing, not heavier weights. b and strong
   are left alone: emphasis inside user content (markdown, messages)
   keeps the browser's bold. */
h1, h2, h3, h4, h5, h6, th {
  font-weight: var(--weight-medium);
}

/* The app type ramp. Headings carry no margin: layouts space them with
   gap (ui.page, card, section), and prose (ui.markdown) sets its own. An
   h1 is a page title at 28px, not a hero; apps should not restyle it. */
h1, h2, h3, h4, h5, h6 {
  margin: 0;
}

h1 { font-size: var(--font-xl); }
h2 { font-size: var(--font-lg); }
h3 { font-size: var(--font-md); }
h4 { font-size: var(--font-base); }
h5, h6 { font-size: var(--font-sm); }

/* Display type: large headings get the display face, tight tracking and
   a short leading. */
h1, h2 {
  font-family: var(--font-display);
  letter-spacing: var(--tracking-display);
  line-height: var(--leading-display);
}

/* Numbers in columns and counters line up. */
td, th {
  font-variant-numeric: tabular-nums;
}

code, kbd, pre, samp {
  font-family: var(--font-mono);
}

/* One shared focus ring, keyboard only. Components never draw their own
   rings on pointer focus. */
:focus-visible {
  outline: var(--focus-outline);
  outline-offset: 2px;
  box-shadow: none;
}

:focus:not(:focus-visible) {
  outline: none;
  box-shadow: none;
}

[data-no-transitions] *,
[data-no-transitions] *::before,
[data-no-transitions] *::after {
  transition: none !important;
}

/* Reduced motion: keep end states and feedback, drop travel and loops. */
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
    scroll-behavior: auto !important;
  }
}")

(defn collect-component-css
  "Read all .css files from the component source directory."
  [dir]
  (->> (fs/glob dir "*.css")
       (sort-by fs/file-name)
       (map #(slurp (str %)))
       (str/join "\n\n")))

;; ── Sticky-hover guard ───────────────────────────────────────────
;; On touch devices a tap triggers :hover and it sticks (no pointer
;; leave), leaving a highlight glued under the finger. The fix is to
;; gate hover styles behind @media (hover: hover). Rather than rely on
;; every author remembering that, we rewrite the CSS at build time:
;; authors write plain `:hover`, the build makes it touch-safe.

(defn- css-match-close
  "Index of the `}` that closes the block starting at `start` in `s`,
   skipping comments and strings. Returns (count s) if unbalanced."
  [^String s start]
  (let [n (count s)]
    (loop [i start depth 0]
      (if (>= i n)
        n
        (let [c (.charAt s i)]
          (cond
            (and (= c \/) (< (inc i) n) (= (.charAt s (inc i)) \*))
            (let [e (str/index-of s "*/" (+ i 2))] (recur (if e (+ e 2) n) depth))
            (or (= c \") (= c \'))
            (recur (loop [j (inc i)]
                     (cond (>= j n) n
                           (= (.charAt s j) \\) (recur (+ j 2))
                           (= (.charAt s j) c) (inc j)
                           :else (recur (inc j))))
                   depth)
            (= c \{) (recur (inc i) (inc depth))
            (= c \}) (if (zero? depth) i (recur (inc i) (dec depth)))
            :else (recur (inc i) depth)))))))

(defn- css-parse-blocks
  "Split a CSS body into segments: {:type :text :raw s} for trailing
   text, or {:type :block :prelude p :inner i}. A block's prelude holds
   any preceding whitespace/comments, so formatting round-trips."
  [^String body]
  (let [n (count body)]
    (loop [i 0 seg-start 0 segs []]
      (if (>= i n)
        (if (< seg-start n)
          (conj segs {:type :text :raw (subs body seg-start)})
          segs)
        (let [c (.charAt body i)]
          (cond
            (and (= c \/) (< (inc i) n) (= (.charAt body (inc i)) \*))
            (let [e (str/index-of body "*/" (+ i 2))] (recur (if e (+ e 2) n) seg-start segs))
            (or (= c \") (= c \'))
            (recur (loop [j (inc i)]
                     (cond (>= j n) n
                           (= (.charAt body j) \\) (recur (+ j 2))
                           (= (.charAt body j) c) (inc j)
                           :else (recur (inc j))))
                   seg-start segs)
            (= c \{)
            (let [close (css-match-close body (inc i))]
              (recur (inc close) (inc close)
                     (conj segs {:type :block
                                 :prelude (subs body seg-start i)
                                 :inner (subs body (inc i) close)})))
            :else (recur (inc i) seg-start segs)))))))

(defn- css-split-commas
  "Split a selector list on commas not nested in (), [] or a string."
  [^String s]
  (let [n (count s)]
    (loop [i 0 depth 0 in-str nil start 0 acc []]
      (if (>= i n)
        (conj acc (subs s start))
        (let [c (.charAt s i)]
          (cond
            in-str (recur (inc i) depth (if (= c in-str) nil in-str) start acc)
            (or (= c \") (= c \')) (recur (inc i) depth c start acc)
            (or (= c \() (= c \[)) (recur (inc i) (inc depth) in-str start acc)
            (or (= c \)) (= c \])) (recur (inc i) (dec depth) in-str start acc)
            (and (= c \,) (zero? depth)) (recur (inc i) depth in-str (inc i) (conj acc (subs s start i)))
            :else (recur (inc i) depth in-str start acc)))))))

(defn- css-split-gap
  "Split a prelude into [leading-ws+comments, selector-or-at-rule]."
  [^String prelude]
  (let [n (count prelude)]
    (loop [i 0]
      (if (>= i n)
        [prelude ""]
        (let [c (.charAt prelude i)]
          (cond
            (Character/isWhitespace c) (recur (inc i))
            (and (= c \/) (< (inc i) n) (= (.charAt prelude (inc i)) \*))
            (let [e (str/index-of prelude "*/" (+ i 2))] (recur (if e (+ e 2) n)))
            :else [(subs prelude 0 i) (subs prelude i)]))))))

(defn- css-hover-media? [sel]
  (and (str/starts-with? sel "@")
       (re-find #"@media\b" sel)
       (re-find #"hover\s*:" sel)))

(defn- css-recurse-at? [sel]
  (and (str/starts-with? sel "@")
       (re-find #"^@(media|supports|layer|container)\b" sel)
       (not (css-hover-media? sel))))

(declare wrap-hover-media)

(defn- css-emit-block [gap sel inner]
  (cond
    (str/starts-with? sel "@")
    (if (css-recurse-at? sel)
      (str gap sel "{" (wrap-hover-media inner) "}")
      (str gap sel "{" inner "}"))
    :else
    (let [sels  (map str/trim (css-split-commas sel))
          hov   (filter #(str/includes? % ":hover") sels)
          other (remove #(str/includes? % ":hover") sels)]
      (if (empty? hov)
        (str gap sel "{" inner "}")
        (str gap
             (when (seq other) (str (str/join ", " other) "{" inner "}\n"))
             "@media (hover: hover){" (str/join ", " hov) "{" inner "}}")))))

(defn wrap-hover-media
  "Gate every `:hover` rule behind @media (hover: hover) so hover styles
   don't stick after a tap on touch devices. Grouped selectors are split
   so non-hover parts (e.g. :focus-visible, state classes) stay ungated.
   Idempotent: rules already inside an @media (hover: …) block are left
   untouched. Pure string transform — authors keep writing plain :hover."
  [css]
  (->> (css-parse-blocks css)
       (map (fn [{:keys [type raw prelude inner]}]
              (if (= type :text)
                raw
                (let [[gap sel] (css-split-gap prelude)]
                  (css-emit-block gap sel inner)))))
       (apply str)))

;; ── Classpath helpers ────────────────────────────────────────────

(defn- deep-merge
  "Recursively merge maps. Non-map values in b override a."
  [a b]
  (merge-with (fn [x y]
                (if (and (map? x) (map? y))
                  (deep-merge x y)
                  y))
              a b))

(defn- find-ui-dir
  "Find the ui/ directory on the classpath (works from git deps)."
  []
  (when-let [marker (io/resource "ui/css/gen.clj")]
    (-> (.getFile marker)
        (str/replace #"/css/gen\.clj$" ""))))

(defn- load-default-tokens
  "Load the default tokens.edn from the classpath."
  []
  (some-> (io/resource "theme/tokens.edn") slurp edn/read-string))

;; ── Public API ───────────────────────────────────────────────────

(defn generate-css
  "Generate the full CSS output from parsed token data.
   component-dir defaults to \"src/ui\" (for local dev).

   Layers, in order: :root (semantic light tokens + :static + scales),
   [data-theme=dark], the prefers-color-scheme fallback, [data-accent=*]
   variants, base styles, component CSS."
  ([token-data] (generate-css token-data "src/ui"))
  ([{:keys [tokens static themes scales accents]} component-dir]
   (let [dark-tokens (themable (get themes :dark) "dark")
         scale-vars  (when scales (generate-scales scales))
         root-block  (str ":root {\n" (tokens->css-block (themable tokens "light"))
                          (when (seq static) (str "\n" (tokens->css-block static)))
                          (when scale-vars (str "\n" scale-vars))
                          "\n}")
         dark-attr   (str "[data-theme=\"dark\"] {\n" (tokens->css-block dark-tokens) "\n}")
         dark-media  (str "@media (prefers-color-scheme: dark) {\n"
                          "  :root:not([data-theme=\"light\"]) {\n"
                          (indent-block (tokens->css-block dark-tokens))
                          "\n  }\n}")
         accent-css  (when (seq accents) (generate-accents accents (:color scales)))
         base        (base-css)
         components  (collect-component-css component-dir)]
     (wrap-hover-media
      (str/join "\n\n" (remove nil? [root-block dark-attr dark-media accent-css base components ""]))))))

(defn build-css
  "Generate bundled CSS string with theme tokens and all component styles.
   Finds default tokens and component CSS from the classpath automatically.

   Returns the CSS string. Optionally writes to :output path.

     ;; Default theme
     (build-css)

     ;; Write to file
     (build-css {:output \"resources/public/ui.css\"})

     ;; Custom accent color (deep-merged with defaults)
     (build-css {:scales {:color {:accent {:hue 200 :chroma 0.20
                                           :steps [[500 0.60]]}}}})

     ;; Compact: just hue + chroma-scale (multiplier on default chroma)
     (build-css {:scales {:color {:gray {:hue 255 :chroma-scale 0.5}
                                  :accent {:hue 255 :chroma-scale 0.87}}}})

     ;; Override semantic tokens
     (build-css {:tokens {:accent \"var(--accent-600)\"}
                 :output \"public/ui.css\"})

   Options map keys:
     :output  — file path to write CSS to (optional)
     :scales  — override/extend scale definitions
     :tokens  — override semantic tokens (light theme)
     :themes  — override theme variants (e.g. {:dark {...}})"
  ([] (build-css {}))
  ([{:keys [output] :as opts}]
   (let [defaults   (load-default-tokens)
         overrides  (dissoc opts :output)
         token-data (if (seq overrides)
                      (deep-merge defaults overrides)
                      defaults)
         ui-dir     (find-ui-dir)
         css        (generate-css token-data ui-dir)]
     (when output
       (fs/create-dirs (fs/parent output))
       (spit output css))
     css)))

(defn build-js
  "Return the pre-built JS runtime string for hiccup targets.
   The JS bundle is committed to the repo and loaded from the classpath.

   Returns the JS string. Optionally writes to :output path.

     ;; Get JS string (e.g. to inline in <script>)
     (build-js)

     ;; Write to file
     (build-js {:output \"resources/public/ui-runtime.js\"})"
  ([] (build-js {}))
  ([{:keys [output]}]
   (let [js (some-> (io/resource "ui/ui-runtime.js") slurp)]
     (when (nil? js)
       (throw (ex-info "ui-runtime.js not found on classpath" {})))
     (when output
       (fs/create-dirs (fs/parent output))
       (spit output js))
     js)))

(defn build-theme!
  "Read tokens from file and write generated CSS to output.
   Used by the local bb build-theme task."
  [{:keys [input output]}]
  (let [token-data (read-tokens input)
        css        (generate-css token-data)]
    (fs/create-dirs (fs/parent output))
    (spit output css)
    (println (str "Generated " output " (" (count (str/split-lines css)) " lines)"))))

;; ── Watch ────────────────────────────────────────────────────────

(defn- collect-watch-files
  "Collect all watchable files: framework CSS + tokens from classpath + extra :watch paths."
  [ui-dir watch-paths]
  (let [framework-css (when ui-dir
                        (->> (fs/glob ui-dir "*.css")
                             (map str)))
        tokens-file   (when-let [r (io/resource "theme/tokens.edn")]
                        [(.getFile r)])
        extra         (mapcat (fn [p]
                                (let [f (io/file p)]
                                  (cond
                                    (not (.exists f)) []
                                    (.isDirectory f)  (map str (fs/glob p "**"))
                                    :else             [p])))
                              watch-paths)]
    (distinct (concat framework-css tokens-file extra))))

(defn- get-mtimes [paths]
  (into {}
    (keep (fn [p]
            (when (fs/exists? p)
              [p (fs/last-modified-time p)])))
    paths))

(defn watch-css
  "Watch files and rebuild CSS on changes. Blocks the current thread.

   Takes the same options as build-css, plus:
     :watch      — vector of extra file paths or directories to watch
     :interval   — poll interval in ms (default 500)
     :on-rebuild — callback (fn [{:keys [output css]}]) called after each successful rebuild

   Automatically watches:
     - All src/ui/*.css component styles from the classpath
     - theme/tokens.edn from the classpath
     - Any extra files/directories in :watch

   :output is required.

   Example bb.edn task:

     watch:css
     {:requires ([ui.css.gen :as css])
      :task (css/watch-css {:output \"resources/public/ui.css\"
                            :watch [\"src/my-overrides.css\"]
                            :scales {:color {:accent {:hue 220}}}})}"
  [{:keys [output watch interval on-rebuild] :as opts}]
  (assert output ":output is required for watch-css")
  (let [build-opts (dissoc opts :watch :interval :on-rebuild)
        ui-dir     (find-ui-dir)
        interval   (or interval 500)]
    ;; Initial build
    (println "[watch-css] Initial build...")
    (let [css (build-css build-opts)]
      (println (str "[watch-css] Wrote " output))
      (when on-rebuild
        (on-rebuild {:output output :css css})))
    (println (str "[watch-css] Watching for changes (poll " interval "ms)..."))
    (loop [prev (get-mtimes (collect-watch-files ui-dir (or watch [])))]
      (Thread/sleep interval)
      (let [files (collect-watch-files ui-dir (or watch []))
            curr  (get-mtimes files)]
        (when (not= prev curr)
          (let [changed (->> (keys curr)
                             (filter #(not= (get prev %) (get curr %)))
                             (map #(fs/file-name %)))]
            (println (str "[watch-css] Changed: " (str/join ", " changed))))
          (try
            (let [css (build-css build-opts)]
              (println (str "[watch-css] Rebuilt " output))
              (when on-rebuild
                (on-rebuild {:output output :css css})))
            (catch Exception e
              (println (str "[watch-css] ERROR: " (.getMessage e))))))
        (recur curr)))))
