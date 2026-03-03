(ns ui.css.gen
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.string :as str]))

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

(defn base-css
  "Generate base body/reset styles."
  []
  "body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  margin: 0;
  background: var(--bg-0);
  color: var(--fg-0);
  transition: background-color 0.2s, color 0.2s;
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
  [{:keys [tokens themes]}]
  (let [dark-tokens (get themes :dark)
        root-block  (str ":root {\n" (tokens->css-block tokens) "\n}")
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
