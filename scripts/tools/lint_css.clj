(ns tools.lint-css
  "Design-system lint for src/ui/*.css (bb lint-css).

   Errors (exit 1): raw hex colors, literal transition durations and
   easings, weights above 500, uppercase transforms, hand-made focus
   outlines. Warnings: raw rem values.

   Append `/* lint-allow */` to a line to exempt it (e.g. the camera's
   black backdrop or the color picker's checkerboard)."
  (:require [babashka.fs :as fs]
            [clojure.string :as str]))

(def rules
  [{:id :hex        :level :error :re #"#[0-9a-fA-F]{3,8}\b"
    :msg "raw hex color — use a token or scale variable"}
   {:id :duration   :level :error :re #"transition[^;]*\b\d*\.?\d+(ms|s)\b"
    :msg "literal duration — use var(--duration-fast|standard|considered)"}
   {:id :easing     :level :error :re #"transition[^;]*(?<![\w-])(ease|ease-in|ease-out|ease-in-out)(?![\w-])"
    :msg "literal easing — use var(--ease-standard|enter|exit|spring)"}
   {:id :weight     :level :error :re #"font-weight:\s*(600|700|800|900|bold)\b"
    :msg "weight above 500 — only 400 and 500 (var(--weight-medium))"}
   {:id :uppercase  :level :error :re #"text-transform:\s*uppercase"
    :msg "uppercase label — sentence case, use size/color for hierarchy"}
   {:id :outline    :level :error :re #"outline:\s*\d+px\s+solid"
    :msg "hand-made focus ring — use var(--focus-outline)"}
   {:id :rem        :level :warn  :re #"(?<![\w-])\d*\.?\d+rem\b"
    :msg "raw rem — prefer var(--size-N) / var(--font-*)"}])

(defn- lint-file [path]
  (let [name (fs/file-name path)]
    (for [[i line] (map-indexed vector (str/split-lines (slurp (str path))))
          :when (not (str/includes? line "lint-allow"))
          :let [code (first (str/split line #"/\*" 2))]
          rule rules
          :when (re-find (:re rule) code)]
      (assoc rule :file name :line (inc i) :text (str/trim line)))))

(defn lint
  "Lint every .css file under dir. Returns {:errors [..] :warnings [..]}."
  [dir]
  (let [hits (mapcat lint-file (sort (fs/glob dir "*.css")))]
    {:errors   (filter #(= :error (:level %)) hits)
     :warnings (filter #(= :warn (:level %)) hits)}))

(defn- print-hits [hits]
  (doseq [{:keys [file line text msg]} hits]
    (println (format "  %s:%d  %s\n      %s" file line text msg))))

(defn lint!
  "CLI entry: prints hits, exits 1 on errors. `--warn` also prints warnings."
  [args]
  (let [{:keys [errors warnings]} (lint "src/ui")
        warn? (some #{"--warn"} args)]
    (when (seq errors)
      (println (str (count errors) " error(s):"))
      (print-hits errors))
    (if warn?
      (do (println (str (count warnings) " warning(s):"))
          (print-hits warnings))
      (println (str (count warnings) " warning(s) (raw rem) — run with --warn to list")))
    (if (seq errors)
      (System/exit 1)
      (println "lint-css: ok"))))
