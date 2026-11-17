(ns ui.filters-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.filters :as filters]))

(defn- classes
  "Every :class string in the hiccup tree."
  [tree]
  (->> (tree-seq coll? seq tree)
       (keep (fn [x] (when (map? x) (:class x))))
       set))

(defn- find-el
  "The first element whose :class is `cls`."
  [tree cls]
  (->> (tree-seq coll? seq tree)
       (some (fn [x] (when (and (vector? x) (map? (second x)) (= cls (:class (second x)))) x)))))

(deftest filters-test
  (testing "the surface"
    (is (= ["filters"] (filters/filters-class-list {})))
    (is (= "filters filters-bare filters-dense" (filters/filters-classes {:bare true :dense true})))
    (let [result (filters/filters {:class "x" :attrs {:id "f"}} [:div "a"] nil [:div "b"])]
      (is (= :div (first result)))
      (is (= "filters x" (get-in result [1 :class])))
      (is (= "f" (get-in result [1 :id])))
      (is (= [[:div "a"] [:div "b"]] (subvec result 2)) "nil bands are dropped"))))

(deftest filter-band-test
  (testing "a labeled band"
    (let [result (filters/filter-band {:label "Rules"} [:b "x"] nil)]
      (is (= "filter-band" (get-in result [1 :class])))
      (is (= [:span {:class "filter-band-label"} "Rules"] (nth result 2)))
      (is (= [:div {:class "filter-band-body"} [:b "x"]] (nth result 3)))))
  (testing "without a label the band is wide"
    (let [result (filters/filter-band {} [:b "x"])]
      (is (= "filter-band filter-band-wide" (get-in result [1 :class])))
      (is (nil? (nth result 2))))))

(deftest filter-footer-test
  (let [result (filters/filter-footer {} "Own view" (filters/filter-action {:disabled true} "Update")
                                      (filters/filter-spacer))]
    (is (= "filter-footer" (get-in result [1 :class])))
    (is (= "Own view" (nth result 2)))
    (is (= [:button {:class "filter-action" :type "button" :disabled true} "Update"] (nth result 3)))
    (is (= [:span {:class "filter-spacer"}] (nth result 4)))))

(deftest filter-disclosure-test
  (testing "details with the summary in words"
    (let [result (filters/filter-disclosure {:summary "15+ comments" :summary-attrs {:id "sum"}} [:div "opts"])]
      (is (= :details (first result)))
      (is (= "filter-disclosure" (get-in result [1 :class])))
      (is (nil? (get-in result [1 :open])))
      (is (= [:span {:class "filter-disclosure-text" :id "sum"} "15+ comments"]
             (find-el result "filter-disclosure-text")))
      (is (= [:div {:class "filter-disclosure-body"} [:div "opts"]] (last result)))))
  (testing ":open"
    (is (true? (get-in (filters/filter-disclosure {:summary "s" :open true}) [1 :open])))))

(deftest preset-test
  (testing "class list"
    (is (= ["preset"] (filters/preset-class-list {})))
    (is (= ["preset" "preset-active"] (filters/preset-class-list {:active true})))
    (is (= ["preset"] (filters/preset-class-list {:dirty true})) "dirty only counts on the active one")
    (is (= "preset preset-active preset-dirty" (filters/preset-classes {:active true :dirty true}))))
  (testing "a plain preset is a wrapper with one button"
    (let [result (filters/preset {:attrs {:data-id "a"} :button-attrs {:hx-get "/p/a"}} "Newest")]
      (is (= :span (first result)))
      (is (= "a" (get-in result [1 :data-id])))
      (is (= [:button {:class "preset-main" :type "button" :hx-get "/p/a"} "Newest"] (nth result 2)))
      (is (= 3 (count result)))))
  (testing ":href makes it a link, the active one is current"
    (let [main (nth (filters/preset {:href "/v/a" :active true} "A") 2)]
      (is (= :a (first main)))
      (is (= {:class "preset-main" :href "/v/a" :aria-current "true"} (second main)))))
  (testing "the changed preset carries a dot, save and revert"
    (let [result (filters/preset {:active true :dirty true :save-attrs {:hx-post "/save"}
                                  :revert-attrs {} :save-label "Save view"}
                   "JQ")
          cs (classes result)]
      (is (contains? cs "preset-dot"))
      (is (= [:button {:class "preset-save" :type "button" :hx-post "/save"} "Save view"]
             (find-el result "preset-save")))
      (is (= "Revert changes" (get-in (find-el result "preset-revert") [1 :aria-label])))))
  (testing "no save without a way to act on it, none on a clean preset"
    (is (not (contains? (classes (filters/preset {:active true :dirty true} "JQ")) "preset-save")))
    (is (not (contains? (classes (filters/preset {:active true :save-attrs {}} "JQ")) "preset-save")))))

(deftest preset-bar-test
  (let [result (filters/preset-bar {} (filters/preset {} "A") nil (filters/preset-add {}))]
    (is (= "preset-bar" (get-in result [1 :class])))
    (is (= "group" (get-in result [1 :role])))
    (is (= 4 (count result)))
    (is (= "Save as new preset" (get-in (last result) [1 :aria-label]))))
  (testing "the menu of every preset"
    (let [result (filters/preset-more {:attrs {:onclick "m()"}})]
      (is (= "preset-add" (get-in result [1 :class])))
      (is (= "All presets" (get-in result [1 :aria-label])))
      (is (= "menu" (get-in result [1 :aria-haspopup])))
      (is (= "m()" (get-in result [1 :onclick])))))
  (testing "a labeled add"
    (let [result (filters/preset-add {:label "Save"} "Save as preset")]
      (is (= "preset-add preset-add-labeled" (get-in result [1 :class])))
      (is (= "Save as preset" (last result))))))

(deftest rule-test
  (testing "class list"
    (is (= ["rule"] (filters/rule-class-list {})))
    (is (= "rule rule-off" (filters/rule-classes {:off true}))))
  (testing "a sentence: grip, verb, words, end"
    (let [result (filters/rule {:verb "Hide" :icon :eye-off :remove-attrs {}}
                   "where" (filters/rule-value {} "Title") nil)
          words (find-el result "rule-words")]
      (is (= "rule" (get-in result [1 :class])))
      (is (= "rule-grip" (get-in result [2 1 :class])))
      (is (= :span (first (find-el result "rule-verb"))) "a plain verb is not a button")
      (is (= "Hide" (last (find-el result "rule-verb"))))
      (is (= [:span {:class "rule-word"} "where"] (nth words 2)) "strings are connectives")
      (is (= [:button {:class "rule-value" :type "button"} "Title"] (nth words 3)))
      (is (= 4 (count words)) "nil words are dropped")
      (is (some? (find-el result "rule-remove")))
      (is (contains? (classes result) "switch switch--sm"))
      (is (contains? (classes result) "switch-track switch-track--checked"))))
  (testing "a paused rule"
    (let [result (filters/rule {:verb "Hide" :off true})]
      (is (= "rule rule-off" (get-in result [1 :class])))
      (is (contains? (classes result) "switch-track"))
      (is (nil? (find-el result "rule-remove")) "no remove without a handler")))
  (testing "parts can be left out"
    (let [cs (classes (filters/rule {:verb "Sort by" :grip false :toggle false}))]
      (is (not (contains? cs "rule-grip")))
      (is (not (contains? cs "switch switch--sm")))))
  (testing "verb attrs make the verb a button"
    (is (= :button (first (find-el (filters/rule {:verb "Hide" :verb-attrs {:onclick "x()"}}) "rule-verb"))))))

(deftest rule-parts-test
  (testing "rule-list"
    (let [result (filters/rule-list {} [:div "r"] nil)]
      (is (= "rule-list" (get-in result [1 :class])))
      (is (= 3 (count result)))))
  (testing "rule-text"
    (is (= [:input {:class "rule-text" :type "text" :value "live" :autocomplete "off" :placeholder "Text"}]
           (filters/rule-text {:value "live" :placeholder "Text"})))
    (is (= "" (get-in (filters/rule-text {}) [1 :value]))))
  (testing "rule-add"
    (let [result (filters/rule-add {:hint "Sort, hide"})]
      (is (= "rule-add" (get-in result [1 :class])))
      (is (= "Add rule" (nth result 3)))
      (is (= [:span {:class "rule-add-hint"} "Sort, hide"] (last result))))
    (is (= "Add filter" (last (filters/rule-add {} "Add filter")))))
  (testing "rule-empty"
    (is (= [:div {:class "rule-empty"} "No rules."] (filters/rule-empty {} "No rules.")))))

(deftest filter-toggle-test
  (testing "class list"
    (is (= ["filter-toggle"] (filters/filter-toggle-class-list {})))
    (is (= "filter-toggle filter-toggle-order filter-toggle-on"
           (filters/filter-toggle-classes {:on true :reorder true}))))
  (testing "a toggle is a pressed button"
    (let [result (filters/filter-toggle {:on true :attrs {:hx-get "/f?c=15"}} "15+")]
      (is (= [:button {:class "filter-toggle filter-toggle-on" :type "button" :aria-pressed "true"
                       :hx-get "/f?c=15"}
              "15+"]
             result)))
    (is (= "false" (get-in (filters/filter-toggle {} "Any") [1 :aria-pressed]))))
  (testing "the column form shows a grip while shown, a crossed eye while hidden"
    (is (= 4 (count (filters/filter-toggle {:reorder true :on true} "Title"))))
    (is (= 4 (count (filters/filter-toggle {:reorder true} "Plays")))))
  (testing "a value splits the pill"
    (let [result (filters/filter-toggle {:on true :value [:b "v"]} "Body text")]
      (is (= "filter-toggle-split" (get-in result [1 :class])))
      (is (= :button (first (nth result 2))))
      (is (= [:span {:class "filter-toggle-value"} [:b "v"]] (nth result 3)))))
  (testing "filter-add"
    (let [result (filters/filter-add {} "Album")]
      (is (= "filter-add" (get-in result [1 :class])))
      (is (= "Album" (last result))))))
