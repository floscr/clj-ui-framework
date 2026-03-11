(ns ui.theme-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ui.css.gen :as gen]
            [ui.theme :as theme]))

(deftest css-var-test
  (testing "generates correct CSS var reference"
    (is (= "var(--accent)" (theme/css-var :accent)))
    (is (= "var(--bg-0)" (theme/css-var :bg-0)))
    (is (= "var(--shadow-2)" (theme/css-var :shadow-2)))))

(deftest token->css-var-test
  (testing "converts keyword to CSS variable name"
    (is (= "--accent" (gen/token->css-var :accent)))
    (is (= "--bg-0" (gen/token->css-var :bg-0)))))

(deftest generate-color-scale-test
  (testing "generates CSS variables for a color scale"
    (let [scale (gen/generate-color-scale
                  :gray {:hue 240 :saturation 18
                         :steps [[50 97] [950 5]]})]
      (is (str/includes? scale "--gray-50:"))
      (is (str/includes? scale "--gray-950:"))
      (is (str/includes? scale "#"))))

  (testing "per-step saturation override"
    (let [scale (gen/generate-color-scale
                  :accent {:hue 252 :saturation 96
                           :steps [[500 67 96] [400 75 93]]})]
      (is (str/includes? scale "--accent-500:"))
      (is (str/includes? scale "--accent-400:")))))

(deftest generate-css-test
  (let [token-data (gen/read-tokens "src/theme/tokens.edn")
        css (gen/generate-css token-data)]

    (testing "contains :root block"
      (is (str/includes? css ":root {")))

    (testing "contains all base tokens as CSS variables"
      (doseq [token [:bg-0 :bg-1 :bg-2 :fg-0 :fg-1 :fg-2
                      :accent :danger :success
                      :border-0 :border-1 :border-2
                      :shadow-0 :shadow-1 :shadow-2 :shadow-3
                      :radius-sm :radius-md :radius-lg]]
        (is (str/includes? css (str "--" (name token) ":"))
            (str "Missing token: " (name token)))))

    (testing "contains color scale variables"
      (doseq [scale ["gray" "accent" "danger" "success" "warning"]]
        (doseq [step [50 100 200 300 400 500 600 700 800 900 950]]
          (is (str/includes? css (str "--" scale "-" step ":"))
              (str "Missing color: " scale "-" step)))))

    (testing "color scales only in :root, not in dark theme blocks"
      (let [dark-block (second (str/split css #"\[data-theme=\"dark\"\]"))]
        (is (not (str/includes? dark-block "--gray-50:")))
        (is (not (str/includes? dark-block "--accent-500:")))))

    (testing "contains dark theme data attribute selector"
      (is (str/includes? css "[data-theme=\"dark\"]")))

    (testing "contains dark theme media query"
      (is (str/includes? css "@media (prefers-color-scheme: dark)")))

    (testing "dark media query excludes explicit light theme"
      (is (str/includes? css ":root:not([data-theme=\"light\"])")))

    (testing "contains size scale variables"
      (doseq [n (range 1 17)]
        (is (str/includes? css (str "--size-" n ":"))
            (str "Missing size-" n))))

    (testing "contains font scale variables"
      (doseq [label ["xs" "sm" "base" "md" "lg" "xl" "2xl" "3xl"]]
        (is (str/includes? css (str "--font-" label ":"))
            (str "Missing font-" label))))

    (testing "scales only in :root, not in dark theme blocks"
      (let [dark-block (second (str/split css #"\[data-theme=\"dark\"\]"))]
        (is (not (str/includes? dark-block "--size-1:")))
        (is (not (str/includes? dark-block "--font-base:")))))

    (testing "contains button component CSS"
      (is (str/includes? css ".btn {"))
      (is (str/includes? css ".btn-primary {"))
      (is (str/includes? css ".btn-secondary {"))
      (is (str/includes? css ".btn-ghost {"))
      (is (str/includes? css ".btn-danger {"))
      (is (str/includes? css ".btn-sm {"))
      (is (str/includes? css ".btn-lg {"))
      (is (str/includes? css ".btn:disabled {")))))

(deftest tokens-roundtrip-test
  (testing "tokens.edn parses without error"
    (let [data (gen/read-tokens "src/theme/tokens.edn")]
      (is (map? (:tokens data)))
      (is (map? (get-in data [:themes :dark])))))

  (testing "dark theme has same keys as base tokens"
    (let [data (gen/read-tokens "src/theme/tokens.edn")
          base-keys (set (keys (:tokens data)))
          dark-keys (set (keys (get-in data [:themes :dark])))]
      (is (= base-keys dark-keys)
          "Dark theme should override all base tokens"))))
