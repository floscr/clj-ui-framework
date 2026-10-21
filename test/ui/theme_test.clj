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
  (testing "generates OKLCH CSS variables for a color scale"
    (let [scale (gen/generate-color-scale
                  :gray {:hue 285 :chroma 0.025
                         :steps [[50 0.975] [950 0.145]]})]
      (is (str/includes? scale "--gray-50:"))
      (is (str/includes? scale "--gray-950:"))
      (is (str/includes? scale "oklch("))))

  (testing "per-step chroma override"
    (let [scale (gen/generate-color-scale
                  :accent {:hue 286 :chroma 0.23
                           :steps [[500 0.595 0.230] [400 0.690 0.170]]})]
      (is (str/includes? scale "--accent-500:"))
      (is (str/includes? scale "--accent-400:"))
      (is (str/includes? scale "oklch(0.595 0.2300 286.0)"))))

  (testing "oklch->hex roundtrip produces valid hex"
    (let [hex (gen/oklch->hex [0.595 0.23 286])]
      (is (string? hex))
      (is (str/starts-with? hex "#") "should start with #")
      (is (= 7 (count hex))))))

(defn- dark-attr-block
  "The declarations inside the [data-theme=\"dark\"] { ... } block only."
  [css]
  (second (re-find #"(?s)\[data-theme=\"dark\"\] \{([^}]*)\}" css)))

(deftest generate-css-test
  (let [token-data (gen/read-tokens "src/theme/tokens.edn")
        css (gen/generate-css token-data)]

    (testing "contains :root block"
      (is (str/includes? css ":root {")))

    (testing "contains all base tokens as CSS variables"
      (doseq [token [:bg-0 :bg-1 :bg-2 :fg-0 :fg-1 :fg-2
                      :accent :accent-strong :accent-subtle :danger :success
                      :border-subtle :border-0 :border-1 :border-2
                      :shadow-0 :shadow-1 :shadow-2 :shadow-3 :shadow-floating
                      :radius-sm :radius-md :radius-lg
                      :radius-control :radius-panel :radius-surface :radius-pill
                      :control-track :control-thumb :series-1 :series-4 :focus-outline]]
        (is (str/includes? css (str "--" (name token) ":"))
            (str "Missing token: " (name token)))))

    (testing "static tokens (type, motion, control heights) live in :root only"
      (let [root-block (first (str/split css #"\[data-theme=\"dark\"\]"))
            dark-block (dark-attr-block css)]
        (doseq [token [:font-body :font-display :font-mono :weight-medium
                       :duration-fast :duration-standard :duration-considered
                       :ease-standard :ease-enter :control-md]]
          (is (str/includes? root-block (str "--" (name token) ":"))
              (str "Missing static token: " (name token)))
          (is (not (str/includes? dark-block (str "--" (name token) ":")))
              (str "Static token duplicated in dark block: " (name token))))))

    (testing "accent variants"
      (is (str/includes? css "[data-accent=\"blue\"] {"))
      (is (str/includes? css "--accent-500: var(--blue-500);"))
      (is (str/includes? css "[data-accent=\"neutral\"] {"))
      (is (str/includes? css "[data-accent=\"neutral\"][data-theme=\"dark\"] {"))
      (is (str/includes? css "[data-accent=\"neutral\"]:not([data-theme=\"light\"])")))

    (testing "surfaces survive a gray scale override without the 0 step"
      (let [custom (assoc-in token-data [:scales :color :gray :steps]
                             (vec (remove #(zero? (first %)) (get-in token-data [:scales :color :gray :steps]))))
            css2 (gen/generate-css custom)]
        (is (not (str/includes? css2 "--gray-0:")))
        (is (str/includes? css2 "--bg-1: var(--theme-bg-1-light, var(--gray-0, #ffffff));"))))

    (testing "themable tokens read a parametric theme's per-mode property first"
      (is (str/includes? css "--accent: var(--theme-accent-light, var(--accent-500));"))
      (is (str/includes? (dark-attr-block css) "--accent: var(--theme-accent-dark, var(--accent-400));"))
      (is (str/includes? css "--border-1: 1px solid var(--theme-border-1-light, var(--gray-300));"))
      (is (str/includes? css "--shadow-1: 0 1px 3px") "other tokens untouched"))

    (testing "emphasis in content keeps its bold; chrome uses medium"
      (is (str/includes? css "h1, h2, h3, h4, h5, h6, th {"))
      (is (not (re-find #"(?m)^h1, h2, h3, h4, h5, h6,[^{]*\b(b|strong)\b[^{]*\{" css))))

    (testing "base styles: shared focus ring and reduced motion"
      (is (str/includes? css "outline: var(--focus-outline);"))
      (is (str/includes? css "@media (prefers-reduced-motion: reduce)"))
      (is (str/includes? css "font-family: var(--font-body);")))

    (testing "contains color scale variables"
      (doseq [scale ["gray" "accent" "danger" "success" "warning"]]
        (doseq [step [50 100 200 300 400 500 600 700 800 900 950]]
          (is (str/includes? css (str "--" scale "-" step ":"))
              (str "Missing color: " scale "-" step))))
      (is (str/includes? css "--gray-0: oklch(1.000 0.0000") "gray-0 is pure white"))

    (testing "color scales only in :root, not in dark theme blocks"
      (let [dark-block (dark-attr-block css)]
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
      (let [dark-block (dark-attr-block css)]
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
          "Dark theme should override all base tokens")))

  (testing "accent token overrides only name existing semantic tokens"
    (let [data (gen/read-tokens "src/theme/tokens.edn")
          base-keys (set (keys (:tokens data)))]
      (doseq [[accent {:keys [scale tokens dark]}] (:accents data)]
        (if scale
          (is (contains? (get-in data [:scales :color]) scale)
              (str accent " names an unknown scale " scale))
          (do (is (every? base-keys (keys tokens)) (str accent " light overrides unknown tokens"))
              (is (every? base-keys (keys dark)) (str accent " dark overrides unknown tokens"))))))))
