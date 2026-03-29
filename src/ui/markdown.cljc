(ns ui.markdown
  "Minimal markdown-to-hiccup renderer for dev documentation pages.
   Supports: headings, fenced code blocks, tables, unordered lists,
   inline code, bold, paragraphs."
  (:require [clojure.string :as str]))

(defn- parse-inline
  "Replace inline markdown (backticks, bold) with hiccup fragments.
   Returns a vector of strings and hiccup nodes."
  [text]
  (if (str/blank? text)
    [text]
    (loop [remaining text
           result []]
      (if (str/blank? remaining)
        result
        (let [;; Find the next special marker
              code-idx (str/index-of remaining "`")
              bold-idx (str/index-of remaining "**")]
          (cond
            ;; Inline code is nearest
            (and code-idx (or (nil? bold-idx) (<= code-idx bold-idx)))
            (let [before   (subs remaining 0 code-idx)
                  after    (subs remaining (inc code-idx))
                  end-idx  (str/index-of after "`")]
              (if end-idx
                (let [code  (subs after 0 end-idx)
                      rest  (subs after (inc end-idx))]
                  (recur rest (cond-> result
                                (seq before) (conj before)
                                true         (conj [:code {:class "md-inline-code"} code]))))
                ;; No closing backtick — treat as literal
                (conj result remaining)))

            ;; Bold is nearest
            (and bold-idx (or (nil? code-idx) (< bold-idx code-idx)))
            (let [before  (subs remaining 0 bold-idx)
                  after   (subs remaining (+ bold-idx 2))
                  end-idx (str/index-of after "**")]
              (if end-idx
                (let [bold (subs after 0 end-idx)
                      rest (subs after (+ end-idx 2))]
                  (recur rest (cond-> result
                                (seq before) (conj before)
                                true         (conj [:strong bold]))))
                (conj result remaining)))

            :else
            (conj result remaining)))))))

(defn- heading-level [line]
  (let [trimmed (str/triml line)]
    (when (str/starts-with? trimmed "#")
      (let [hashes (re-find #"^#{1,6}" trimmed)]
        (when hashes
          (let [n (count hashes)
                text (str/trim (subs trimmed n))]
            [n text]))))))

(defn- table-row? [line]
  (and (str/starts-with? (str/trim line) "|")
       (str/ends-with? (str/trim line) "|")))

(defn- separator-row? [line]
  (and (table-row? line)
       (every? #(re-matches #"\s*:?-+:?\s*" %)
               (-> (str/trim line)
                   (subs 1)
                   (str/replace #"\|$" "")
                   (str/split #"\|")))))

(defn- parse-table-cells [line]
  (->> (-> (str/trim line)
           (subs 1)
           (str/replace #"\|$" "")
           (str/split #"\|"))
       (mapv str/trim)))

(defn- list-item? [line]
  (re-matches #"^\s*[-*]\s+.+" line))

(defn- list-item-text [line]
  (str/replace line #"^\s*[-*]\s+" ""))

(defn markdown->hiccup
  "Convert a markdown string to a hiccup data structure.
   Returns a vector of hiccup elements."
  [md-str]
  (let [lines (str/split-lines md-str)]
    (loop [i       0
           result  []]
      (if (>= i (count lines))
        result
        (let [line (nth lines i)]
          (cond
            ;; Blank line — skip
            (str/blank? line)
            (recur (inc i) result)

            ;; Fenced code block
            (str/starts-with? (str/trim line) "```")
            (let [lang   (str/replace (str/trim (subs (str/trim line) 3)) #"\"" "")
                  ;; Collect lines until closing ```
                  code-lines (loop [j    (inc i)
                                    acc  []]
                               (if (>= j (count lines))
                                 [acc j]
                                 (let [l (nth lines j)]
                                   (if (str/starts-with? (str/trim l) "```")
                                     [acc (inc j)]
                                     (recur (inc j) (conj acc l))))))]
              (recur (second code-lines)
                     (conj result
                           [:pre {:class "md-code-block"}
                            [:code {:class (when (seq lang) (str "language-" lang))}
                             (str/join "\n" (first code-lines))]])))

            ;; Heading
            (heading-level line)
            (let [[n text] (heading-level line)
                  tag (keyword (str "h" n))]
              (recur (inc i)
                     (conj result (into [tag {:class "md-heading"}] (parse-inline text)))))

            ;; Table
            (table-row? line)
            (let [header-cells (parse-table-cells line)
                  ;; Skip separator row, collect data rows
                  body-start (if (and (< (inc i) (count lines))
                                      (separator-row? (nth lines (inc i))))
                               (+ i 2)
                               (inc i))
                  body-rows  (loop [j   body-start
                                    acc []]
                               (if (and (< j (count lines))
                                        (table-row? (nth lines j)))
                                 (recur (inc j) (conj acc (parse-table-cells (nth lines j))))
                                 [acc j]))]
              (recur (second body-rows)
                     (conj result
                           [:table {:class "md-table"}
                            [:thead
                             (into [:tr]
                                   (map (fn [c] (into [:th] (parse-inline c)))
                                        header-cells))]
                            (into [:tbody]
                                  (map (fn [row]
                                         (into [:tr]
                                               (map (fn [c] (into [:td] (parse-inline c)))
                                                    row)))
                                       (first body-rows)))])))

            ;; Unordered list — collect consecutive items
            (list-item? line)
            (let [items (loop [j   i
                               acc []]
                          (if (and (< j (count lines))
                                   (list-item? (nth lines j)))
                            (recur (inc j) (conj acc (list-item-text (nth lines j))))
                            [acc j]))]
              (recur (second items)
                     (conj result
                           (into [:ul {:class "md-list"}]
                                 (map (fn [txt] (into [:li] (parse-inline txt)))
                                      (first items))))))

            ;; Paragraph (default)
            :else
            (let [;; Collect consecutive non-blank, non-special lines
                  para (loop [j   i
                              acc []]
                         (if (and (< j (count lines))
                                  (not (str/blank? (nth lines j)))
                                  (not (str/starts-with? (str/trim (nth lines j)) "```"))
                                  (not (str/starts-with? (str/trim (nth lines j)) "#"))
                                  (not (table-row? (nth lines j)))
                                  (not (list-item? (nth lines j))))
                           (recur (inc j) (conj acc (nth lines j)))
                           [acc j]))]
              (recur (second para)
                     (conj result
                           (into [:p {:class "md-paragraph"}]
                                 (parse-inline (str/join " " (first para)))))))))))))
