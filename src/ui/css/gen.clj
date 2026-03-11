(ns ui.css.gen
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
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
  "Generate CSS variables for a named color scale.
  Each step is [label lightness] or [label lightness saturation].
  Uses hsl->hex from jon.color-tools for conversion."
  [scale-name {:keys [hue saturation steps]}]
  (->> steps
       (map (fn [step]
              (let [[label lightness sat] (if (= 3 (count step))
                                            step
                                            [(first step) (second step) saturation])
                    hex (color/hsl->hex [hue sat lightness])]
                (str "  --" (name scale-name) "-" label ": " hex ";"))))
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

(defn generate-css
  "Generate the full CSS output from parsed token data."
  [{:keys [tokens themes scales]}]
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
        components  (collect-component-css "src/ui")]
    (str/join "\n\n" [root-block dark-attr dark-media base components ""])))

(defn build-theme!
  "Read tokens from file and write generated CSS to output."
  [{:keys [input output]}]
  (let [token-data (read-tokens input)
        css        (generate-css token-data)]
    (fs/create-dirs (fs/parent output))
    (spit output css)
    (println (str "Generated " output " (" (count (str/split-lines css)) " lines)"))))
