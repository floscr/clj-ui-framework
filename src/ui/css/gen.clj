(ns ui.css.gen
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [jon.color-tools :as color]))

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

(defn oklch->hex
  "Convert OKLCH [L C H] to hex string. Clamps to sRGB gamut."
  [[l c h]]
  (color/rgb->hex (oklch->srgb [l c h])))

;; ── Scale generation ─────────────────────────────────────────────

(defn generate-size-scale
  "Generate linear size scale: --size-N = base * N."
  [{:keys [base unit steps]}]
  (->> (range 1 (inc steps))
       (map (fn [n]
              (str "  --size-" n ": " (format-number (* base n)) unit ";")))
       (str/join "\n")))

(defn generate-font-scale
  "Generate geometric font scale: --font-{name} = base * ratio^power."
  [{:keys [base unit ratio steps]}]
  (->> steps
       (map (fn [[power label]]
              (str "  --font-" label ": "
                   (format-number (* base (Math/pow ratio power)))
                   unit ";")))
       (str/join "\n")))

(defn generate-color-scale
  "Generate CSS variables for a named OKLCH color scale.
   Each step is [label lightness] or [label lightness chroma].
   Outputs oklch() CSS values for perceptual uniformity."
  [scale-name {:keys [hue chroma steps]}]
  (->> steps
       (map (fn [step]
              (let [[label lightness chr] (if (= 3 (count step))
                                            step
                                            [(first step) (second step) chroma])
                    ;; Format: oklch(L C H)
                    css-val (str "oklch(" (format "%.3f" (double lightness))
                                " " (format "%.4f" (double chr))
                                " " (format "%.1f" (double hue)) ")")]
                (str "  --" (name scale-name) "-" label ": " css-val ";"))))
       (str/join "\n")))

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

(defn base-css
  "Generate base body/reset styles."
  []
  "*, *::before, *::after {
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  margin: 0;
  background: var(--bg-0);
  color: var(--fg-0);
}

:focus-visible {
  outline: 2px solid var(--accent);
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
}")

(defn collect-component-css
  "Read all .css files from the component source directory."
  [dir]
  (->> (fs/glob dir "*.css")
       (sort-by fs/file-name)
       (map #(slurp (str %)))
       (str/join "\n\n")))

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
   component-dir defaults to \"src/ui\" (for local dev)."
  ([token-data] (generate-css token-data "src/ui"))
  ([{:keys [tokens themes scales]} component-dir]
   (let [dark-tokens (get themes :dark)
         scale-vars  (when scales (generate-scales scales))
         root-block  (str ":root {\n" (tokens->css-block tokens)
                          (when scale-vars (str "\n" scale-vars))
                          "\n}")
         dark-attr   (str "[data-theme=\"dark\"] {\n" (tokens->css-block dark-tokens) "\n}")
         dark-media  (str "@media (prefers-color-scheme: dark) {\n"
                          "  :root:not([data-theme=\"light\"]) {\n"
                          (str/replace (tokens->css-block dark-tokens) #"(?m)^  " "    ")
                          "\n  }\n}")
         base        (base-css)
         components  (collect-component-css component-dir)]
     (str/join "\n\n" [root-block dark-attr dark-media base components ""]))))

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

(defn build-theme!
  "Read tokens from file and write generated CSS to output.
   Used by the local bb build-theme task."
  [{:keys [input output]}]
  (let [token-data (read-tokens input)
        css        (generate-css token-data)]
    (fs/create-dirs (fs/parent output))
    (spit output css)
    (println (str "Generated " output " (" (count (str/split-lines css)) " lines)"))))
