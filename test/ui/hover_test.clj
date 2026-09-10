(ns ui.hover-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ui.css.gen :as gen]))

(defn- strip-comments [css]
  (str/replace css #"(?s)/\*.*?\*/" ""))

(defn- strip-hover-media
  "Remove every @media (hover: hover){…} block (brace-balanced) from css,
   tolerating any whitespace between the query and its opening brace."
  [css]
  (loop [s css]
    (if-let [m (re-find #"@media \(hover: hover\)\s*\{" s)]
      (let [idx (str/index-of s m)
            open (+ idx (count m) -1)              ; index of the opening {
            close (loop [i (inc open) depth 1]
                    (cond (>= i (count s)) i
                          (= (.charAt s i) \{) (recur (inc i) (inc depth))
                          (= (.charAt s i) \}) (if (= depth 1) i (recur (inc i) (dec depth)))
                          :else (recur (inc i) depth)))]
        (recur (str (subs s 0 idx) (subs s (inc close)))))
      s)))

(defn- no-bare-hover?
  "True if every :hover in the output sits inside an @media (hover: …) block
   (ignoring :hover mentions inside comments)."
  [css]
  (nil? (re-find #":hover"
                 (-> (gen/wrap-hover-media css)
                     strip-comments
                     strip-hover-media))))

(deftest wraps-simple-hover
  (testing "a bare :hover rule is gated behind @media (hover: hover)"
    (let [out (gen/wrap-hover-media ".btn:hover { color: red; }")]
      (is (str/includes? out "@media (hover: hover){"))
      (is (str/includes? out ".btn:hover{ color: red; }")))))

(deftest leaves-non-hover-untouched
  (testing "rules without :hover pass through unchanged"
    (let [css ".btn { color: red; }\n.x:focus { outline: none; }"]
      (is (= css (gen/wrap-hover-media css))))))

(deftest splits-grouped-selectors
  (testing ":hover and :focus-visible grouped — only :hover is gated"
    (let [out (gen/wrap-hover-media
               ".item:hover, .item:focus-visible { background: blue; }")]
      ;; focus-visible stays outside hover media so keyboard focus works on touch
      (is (re-find #"^\.item:focus-visible\{" (str/triml out)))
      (is (str/includes? out "@media (hover: hover){.item:hover{"))
      (is (not (str/includes? out "hover){.item:focus-visible"))))))

(deftest splits-hover-and-state-class
  (testing ":hover grouped with a state class — state class stays ungated"
    (let [out (gen/wrap-hover-media
               ".drop:hover, .drop-active { border-color: red; }")]
      (is (str/includes? out ".drop-active{"))
      (is (str/includes? out "@media (hover: hover){.drop:hover{")))))

(deftest idempotent
  (testing "running twice is a fixed point"
    (let [css ".a:hover { color: red; }\n.b:hover, .b-on { x: 1; }"
          once (gen/wrap-hover-media css)
          twice (gen/wrap-hover-media once)]
      (is (= once twice)))))

(deftest skips-existing-hover-media
  (testing "already-wrapped hover rules are left alone"
    (let [css "@media (hover: hover) {\n  .a:hover { color: red; }\n}"]
      (is (= css (gen/wrap-hover-media css))))))

(deftest recurses-into-dark-media
  (testing ":hover inside a prefers-color-scheme block is still gated"
    (let [css "@media (prefers-color-scheme: dark) {\n  .a:hover { color: red; }\n}"
          out (gen/wrap-hover-media css)]
      (is (str/includes? out "@media (prefers-color-scheme: dark)"))
      (is (str/includes? out "@media (hover: hover){.a:hover{")))))

(deftest ignores-braces-in-comments-and-strings
  (testing "braces inside comments/strings don't break block matching"
    (let [css ".a:hover { content: \"}\"; /* } */ color: red; }\n.b { x: 1; }"
          out (gen/wrap-hover-media css)]
      (is (str/includes? out "@media (hover: hover){.a:hover{"))
      ;; the following non-hover rule survives intact (original spacing kept)
      (is (str/includes? out ".b { x: 1; }"))
      (is (str/includes? out "content: \"}\"")))))

(deftest does-not-split-comma-in-brackets
  (testing "commas inside :not()/[] are not treated as selector separators"
    (let [out (gen/wrap-hover-media
               ".a:hover:not(.x, .y) { color: red; }")]
      ;; the :not() stays one selector inside a single hover-media block
      (is (str/includes? out "@media (hover: hover){.a:hover:not(.x, .y){"))
      ;; exactly one hover-media block (not split into two by the inner comma)
      (is (= 1 (count (re-seq #"@media \(hover: hover\)" out)))))))

(deftest generated-theme-has-no-bare-hover
  (testing "the full generated theme.css has no ungated :hover"
    (is (no-bare-hover? (gen/build-css)))))
