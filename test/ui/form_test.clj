(ns ui.form-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.form :as form]
            [ui.icon :as icon]))

;; ── form-field ──────────────────────────────────────────────────────

(deftest form-field-class-list-test
  (testing "default field"
    (is (= ["form-field"] (form/form-field-class-list {}))))
  (testing "with error"
    (is (= ["form-field" "form-field--error"] (form/form-field-class-list {:error true})))))

(deftest form-field-classes-test
  (testing "default"
    (is (= "form-field" (form/form-field-classes {}))))
  (testing "with error"
    (is (= "form-field form-field--error" (form/form-field-classes {:error true})))))

(deftest form-field-component-test
  (testing "renders with label and child"
    (let [result (form/form-field {:label "Name"} [:input {:type "text"}])]
      (is (= :div (first result)))
      (is (= "form-field" (get-in result [1 :class])))
      ;; label
      (is (= :label (first (nth result 2))))
      (is (= "Name" (nth (nth result 2) 2)))
      ;; child input
      (is (= :input (first (nth result 3))))))

  (testing "renders hint text"
    (let [result (form/form-field {:label "Password" :hint "At least 8 characters"} [:input])]
      (is (some #(and (vector? %) (= :small (first %)) (= "form-hint" (get-in % [1 :class])))
                result))))

  (testing "renders error icon with tooltip"
    (let [result (form/form-field {:label "Email" :error "Invalid email"} [:input])]
      (is (= "form-field form-field--error" (get-in result [1 :class])))
      ;; Children wrapped in form-field-control
      (let [control (nth result 3)]
        (is (= :div (first control)))
        (is (= "form-field-control" (get-in control [1 :class])))
        ;; Contains the child input
        (is (= :input (first (nth control 2))))
        ;; Contains the tooltip with error text
        (let [tip (nth control 3)]
          (is (= :span (first tip)))
          (is (= "Invalid email" (get-in tip [1 :data-tooltip])))))))

  (testing "no label renders without label element"
    (let [result (form/form-field {} [:input])]
      (is (= :input (first (nth result 2))))))

  (testing "extra class appended"
    (let [result (form/form-field {:class "extra"} [:input])]
      (is (= "form-field extra" (get-in result [1 :class]))))))

;; ── form-input ──────────────────────────────────────────────────────

(deftest form-input-class-list-test
  (testing "default"
    (is (= ["form-input"] (form/form-input-class-list {}))))
  (testing "with error"
    (is (= ["form-input" "form-input--error"] (form/form-input-class-list {:error true})))))

(deftest form-input-component-test
  (testing "basic text input"
    (let [result (form/form-input {:type :text :placeholder "Name"})]
      (is (= :input (first result)))
      (is (= "form-input" (get-in result [1 :class])))
      (is (= "text" (get-in result [1 :type])))
      (is (= "Name" (get-in result [1 :placeholder])))))

  (testing "email type"
    (let [result (form/form-input {:type :email})]
      (is (= "email" (get-in result [1 :type])))))

  (testing "password type"
    (let [result (form/form-input {:type :password})]
      (is (= "password" (get-in result [1 :type])))))

  (testing "date type"
    (let [result (form/form-input {:type :date})]
      (is (= "date" (get-in result [1 :type])))))

  (testing "datetime-local type"
    (let [result (form/form-input {:type :datetime-local})]
      (is (= "datetime-local" (get-in result [1 :type])))))

  (testing "default type is text"
    (let [result (form/form-input {})]
      (is (= "text" (get-in result [1 :type])))))

  (testing "disabled"
    (let [result (form/form-input {:disabled true})]
      (is (true? (get-in result [1 :disabled])))))

  (testing "with value"
    (let [result (form/form-input {:value "hello"})]
      (is (= "hello" (get-in result [1 :value])))))

  (testing "error styling"
    (let [result (form/form-input {:error true})]
      (is (= "form-input form-input--error" (get-in result [1 :class]))))))

(deftest form-input-icon-test
  (testing "input with icon-left wraps in form-input-wrap"
    (let [result (form/form-input {:icon-left :search :placeholder "Search..."})]
      (is (= :div (first result)))
      (is (= "form-input-wrap" (get-in result [1 :class])))
      ;; first child: left icon span
      (let [icon-span (nth result 2)]
        (is (= :span (first icon-span)))
        (is (= "form-input-icon form-input-icon--left" (get-in icon-span [1 :class]))))
      ;; second child: the actual input
      (let [input (nth result 3)]
        (is (= :input (first input)))
        (is (clojure.string/includes? (get-in input [1 :class]) "form-input--icon-left")))))

  (testing "input with icon-right wraps in form-input-wrap"
    (let [result (form/form-input {:icon-right :check})]
      (is (= :div (first result)))
      ;; second child: the input
      (let [input (nth result 2)]
        (is (= :input (first input)))
        (is (clojure.string/includes? (get-in input [1 :class]) "form-input--icon-right")))
      ;; third child: right icon span
      (let [icon-span (nth result 3)]
        (is (= :span (first icon-span)))
        (is (= "form-input-icon form-input-icon--right" (get-in icon-span [1 :class]))))))

  (testing "input with both icons"
    (let [result (form/form-input {:icon-left :search :icon-right :x})]
      (is (= :div (first result)))
      (is (= 5 (count result)))))  ;; div + attrs + left-icon + input + right-icon

  (testing "input without icons returns bare input"
    (let [result (form/form-input {:placeholder "Name"})]
      (is (= :input (first result))))))

;; ── form-textarea ───────────────────────────────────────────────────

(deftest form-textarea-class-list-test
  (testing "default"
    (is (= ["form-textarea"] (form/form-textarea-class-list {}))))
  (testing "with error"
    (is (= ["form-textarea" "form-textarea--error"] (form/form-textarea-class-list {:error true})))))

(deftest form-textarea-component-test
  (testing "basic textarea"
    (let [result (form/form-textarea {:placeholder "Message"})]
      (is (= :textarea (first result)))
      (is (= "form-textarea" (get-in result [1 :class])))
      (is (= "Message" (get-in result [1 :placeholder])))))

  (testing "disabled"
    (let [result (form/form-textarea {:disabled true})]
      (is (true? (get-in result [1 :disabled])))))

  (testing "with value"
    (let [result (form/form-textarea {:value "hello"})]
      (is (= "hello" (nth result 2))))))

;; ── form-select ─────────────────────────────────────────────────────

(deftest form-select-component-test
  (testing "basic select delegates to the custom ui.select trigger"
    (let [result  (form/form-select {:options [{:value "a" :label "Option A"}
                                               {:value "b" :label "Option B"}]})
          trigger (nth result 2)]
      (is (= :div (first result)))
      (is (= "select" (get-in result [1 :class])))
      (is (= :button (first trigger)))
      (is (= "select-trigger" (get-in trigger [1 :class])))
      ;; options are serialized for the JS runtime
      (is (re-find #"Option A" (get-in trigger [1 :data-select-options])))
      (is (re-find #"Option B" (get-in trigger [1 :data-select-options])))))

  (testing "with placeholder shows it as the trigger label"
    (let [result  (form/form-select {:placeholder "Pick one"
                                     :options [{:value "a" :label "A"}]})
          trigger (nth result 2)
          span    (nth trigger 2)]
      (is (= "Pick one" (nth span 2)))))

  (testing "selected value shows its label"
    (let [result  (form/form-select {:value "b"
                                     :options [{:value "a" :label "A"}
                                               {:value "b" :label "Bee"}]})
          trigger (nth result 2)
          span    (nth trigger 2)]
      (is (= "Bee" (nth span 2)))
      (is (= "b" (get-in trigger [1 :data-select-value])))))

  (testing "disabled"
    (let [result  (form/form-select {:disabled true :options []})
          trigger (nth result 2)]
      (is (true? (get-in trigger [1 :disabled])))))

  (testing "name emits a hidden input"
    (let [result (form/form-select {:name "role" :value "a"
                                    :options [{:value "a" :label "A"}]})
          input  (nth result 3)]
      (is (= :input (first input)))
      (is (= "hidden" (get-in input [1 :type])))
      (is (= "role" (get-in input [1 :name])))
      (is (= "a" (get-in input [1 :value])))))

  (testing "string options"
    (let [result  (form/form-select {:options ["Foo" "Bar"]})
          trigger (nth result 2)]
      (is (re-find #"Foo" (get-in trigger [1 :data-select-options])))
      (is (re-find #"Bar" (get-in trigger [1 :data-select-options]))))))

;; ── form-checkbox ───────────────────────────────────────────────────

(deftest form-checkbox-component-test
  (testing "basic checkbox"
    (let [result (form/form-checkbox {:label "Agree"})]
      (is (= :label (first result)))
      (is (= "form-field form-field--inline" (get-in result [1 :class])))
      ;; has input checkbox
      (let [input (nth result 2)]
        (is (= :input (first input)))
        (is (= "checkbox" (get-in input [1 :type])))
        (is (= "form-checkbox" (get-in input [1 :class]))))
      ;; has label text
      (is (= [:span "Agree"] (nth result 3)))))

  (testing "checked"
    (let [result (form/form-checkbox {:label "Agree" :checked true})
          input (nth result 2)]
      (is (true? (get-in input [1 :checked])))))

  (testing "disabled"
    (let [result (form/form-checkbox {:disabled true})
          input (nth result 2)]
      (is (true? (get-in input [1 :disabled]))))))

;; ── form-radio-group ────────────────────────────────────────────────

(deftest form-radio-group-component-test
  (testing "basic radio group"
    (let [result (form/form-radio-group {:label "Preference"
                                         :radio-name "pref"
                                         :options [{:value "a" :label "A"}
                                                   {:value "b" :label "B"}
                                                   {:value "c" :label "C"}]})]
      (is (= :fieldset (first result)))
      (is (= "form-fieldset form-fieldset--inline" (get-in result [1 :class])))
      ;; legend
      (is (= :legend (first (nth result 2))))
      (is (= "Preference" (nth (nth result 2) 2)))
      ;; three radio labels
      (is (= 6 (count result))))) ; fieldset + attrs + legend + 3 radios

  (testing "radio with selected value"
    (let [result (form/form-radio-group {:radio-name "pref"
                                         :radio-value "b"
                                         :options [{:value "a" :label "A"}
                                                   {:value "b" :label "B"}]})
          ;; find the radio for "b"
          radio-b (nth result 3) ; second radio (after attrs, no legend since no :label)
          input-b (nth radio-b 1)]
      (is (true? (get-in input-b [1 :checked])))))

  (testing "string options"
    (let [result (form/form-radio-group {:radio-name "x" :options ["X" "Y"]})]
      ;; 2 radios + attrs = 4 elements
      (is (= 4 (count result))))))

;; ── form-file ───────────────────────────────────────────────────────

(deftest form-file-component-test
  (testing "basic file input"
    (let [result (form/form-file {})]
      (is (= :input (first result)))
      (is (= "file" (get-in result [1 :type])))
      (is (= "form-file" (get-in result [1 :class])))))

  (testing "with accept"
    (let [result (form/form-file {:accept "image/*"})]
      (is (= "image/*" (get-in result [1 :accept])))))

  (testing "multiple"
    (let [result (form/form-file {:multiple true})]
      (is (true? (get-in result [1 :multiple])))))

  (testing "disabled"
    (let [result (form/form-file {:disabled true})]
      (is (true? (get-in result [1 :disabled]))))))

;; ── form-range ──────────────────────────────────────────────────────

(deftest form-range-component-test
  (testing "basic range"
    (let [result (form/form-range {:min 0 :max 100 :value 50})]
      (is (= :input (first result)))
      (is (= "range" (get-in result [1 :type])))
      (is (= "form-range" (get-in result [1 :class])))
      (is (= 0 (get-in result [1 :min])))
      (is (= 100 (get-in result [1 :max])))
      (is (= 50 (get-in result [1 :value])))))

  (testing "with step"
    (let [result (form/form-range {:step 5})]
      (is (= 5 (get-in result [1 :step])))))

  (testing "disabled"
    (let [result (form/form-range {:disabled true})]
      (is (true? (get-in result [1 :disabled]))))))

;; ── form-group ──────────────────────────────────────────────────────

(deftest form-group-component-test
  (testing "basic group"
    (let [result (form/form-group {} [:input] [:button "Go"])]
      (is (= :div (first result)))
      (is (= "form-group" (get-in result [1 :class])))
      (is (= 4 (count result))))))

(deftest form-group-addon-component-test
  (testing "basic addon"
    (let [result (form/form-group-addon {} "https://")]
      (is (= :span (first result)))
      (is (= "form-group-addon" (get-in result [1 :class])))
      (is (= "https://" (nth result 2))))))

;; ── search-bar preset ───────────────────────────────────────────────

(deftest search-bar-defaults-test
  (testing "without :action renders a bare form-group"
    (let [result (form/search-bar {})]
      (is (= :div (first result)))
      (is (= "form-group" (get-in result [1 :class])))))
  (testing "input is icon-wrapped with defaults"
    (let [group (form/search-bar {})
          wrap  (nth group 2)
          input (nth wrap 3)]
      (is (= "form-input-wrap" (get-in wrap [1 :class])))
      (is (= "q" (get-in input [1 :name])))
      (is (= "Search\u2026" (get-in input [1 :placeholder])))
      (is (clojure.string/includes? (get-in input [1 :class]) "form-input--icon-left"))))
  (testing "submit button defaults to primary + type=submit"
    (let [group  (form/search-bar {})
          button (nth group 3)]
      (is (clojure.string/includes? (get-in button [1 :class]) "btn-primary"))
      (is (= "submit" (get-in button [1 :type])))
      (is (= "Search" (last button))))))

(deftest search-bar-custom-test
  (testing "custom name, placeholder, label"
    (let [group  (form/search-bar {:name "query" :placeholder "Find\u2026"
                                   :button-label "Go"})
          wrap   (nth group 2)
          input  (nth wrap 3)
          button (nth group 3)]
      (is (= "query" (get-in input [1 :name])))
      (is (= "Find\u2026" (get-in input [1 :placeholder])))
      (is (= "Go" (last button)))))
  (testing "value is passed through"
    (let [group (form/search-bar {:value "cats"})
          input (nth (nth group 2) 3)]
      (is (= "cats" (get-in input [1 :value])))))
  (testing "disabled disables input and button"
    (let [group  (form/search-bar {:disabled true})
          input  (nth (nth group 2) 3)
          button (nth group 3)]
      (is (true? (get-in input [1 :disabled])))
      (is (true? (get-in button [1 :disabled]))))))

(deftest search-bar-form-wrapper-test
  (testing ":action wraps in a GET form"
    (let [result (form/search-bar {:action "/search"})]
      (is (= :form (first result)))
      (is (= "/search" (get-in result [1 :action])))
      (is (= "get" (get-in result [1 :method])))
      (is (= "form-group" (get-in (nth result 2) [1 :class])))))
  (testing ":form-attrs merge onto the form"
    (let [result (form/search-bar {:action "/s" :form-attrs {:style "flex:1;"}})]
      (is (= "flex:1;" (get-in result [1 :style]))))))
