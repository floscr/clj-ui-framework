(ns ui.theme-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.theme-model :as model]
            [ui.themes :as themes]))

(deftest defaults-reproduce-the-generated-theme
  (let [vars (model/css-vars model/defaults)]
    (testing "same format and values as ui.css.gen for tokens.edn"
      (is (= "oklch(1.000 0.0000 285.0)" (get vars "--gray-0")))
      (is (= "oklch(0.530 0.0350 285.0)" (get vars "--gray-500")))
      (is (= "oklch(0.595 0.2300 286.0)" (get vars "--accent-500")))
      (is (= "oklch(0.705 0.1850 152.0)" (get vars "--success-500")))
      (is (= "0.75rem" (get vars "--font-xs")))
      (is (= "0.813rem" (get vars "--font-sm")))
      (is (= "0.875rem" (get vars "--font-base")))
      (is (= "1rem" (get vars "--font-md")))
      (is (= "1.75rem" (get vars "--font-xl")))
      (is (= "0.25rem" (get vars "--size-1")))
      (is (= "8px" (get vars "--radius-md")))
      (is (= "9px" (get vars "--radius-control"))))
    (testing "no fonts and no exact colors without a palette"
      (is (nil? (get vars "--font-body")))
      (is (nil? (get vars "--theme-accent-light"))))
    (testing "a dark page's surfaces sit on it, text on gray-50"
      (is (= "oklch(0.975 0.0016 285.0)" (get vars "--theme-fg-0-dark")))
      (is (= (get vars "--theme-bg-0-dark") "oklch(0.147 0.0107 285.0)")))
    (testing "var-names covers every property a theme may set"
      (is (every? (set model/var-names) (keys vars)))
      (is (some #{"--theme-accent-dark"} model/var-names))
      (is (some #{"--font-mono"} model/var-names)))))

(deftest colors
  (is (= [0 0 0] (mapv #(Math/round (* 1000 (double %))) (model/hex->oklch "#000000"))))
  (is (model/color? "#a1b2c3"))
  (is (model/color? "oklch(0.5 0.1 120)"))
  (is (not (model/color? "red")))
  (is (= "oklch(0.500 0.0000 0.0)" (model/mix "#000000" "#ffffff" 0.5)))
  (testing "text on a pastel fill is dark, on a deep one white"
    (is (= "#ffffff" (model/contrast-text "#0969da")))
    (is (not= "#ffffff" (model/contrast-text "#cba6f7")))))

(deftest palettes-win-over-derived-tokens
  (let [p      {:bg-dark "#282828" :dark {:fg "#ebdbb2" :accent "#fabd2f" :border "#504945"}}
        dark   (:dark (model/mode-tokens p))
        light  (:light (model/mode-tokens p))]
    (is (= "#ebdbb2" (dark "fg-0")))
    (is (not= (dark "fg-0") (dark "fg-1")) "muted text mixed toward the page")
    (is (= "#fabd2f" (dark "accent")))
    (is (some? (dark "accent-strong")))
    (is (not= "#ffffff" (dark "fg-on-accent")) "a light accent takes dark text")
    (is (= "#504945" (dark "border-1")))
    (is (nil? (light "accent")) "the other mode stays derived")
    (is (= "#fabd2f" (get (model/css-vars p) "--theme-accent-dark")))))

(deftest fonts
  (is (= ["JetBrains Mono" "IBM Plex Mono" "monospace"]
         (model/font-families "JetBrains Mono, 'IBM Plex Mono', monospace")))
  (is (nil? (model/font-families "Inter; color: red")))
  (is (nil? (model/font-families " , ")))
  (is (= (str "\"Inter\", sans-serif, " (:font-body model/default-fonts))
         (model/font-stack :font-body "Inter, sans-serif")))
  (is (= "\"Iosevka\", ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace"
         (get (model/css-vars {:font-mono "Iosevka"}) "--font-mono"))))

(deftest normalize-params
  (is (= {:gray-hue 120 :bg-dark "#000000" :font-body "Inter"
          :dark {:fg "#ffffff"}}
         (model/normalize-params {:gray-hue 120 :accent-hue 999 :bg-dark "#000000"
                                  :bg-light "white" :font-body " Inter " :font-mono "a{b}"
                                  :dark {:fg "#ffffff" :nope "#000000" :accent "blue"}
                                  :light {:fg "nope"} :unknown 1})))
  (testing "a consumer's own parameters"
    (is (= {:code-scale 1.2 :diff-color "#00ff00"}
           (model/normalize-params {:code-scale 1.2 :diff-color "#00ff00"}
                                   {:ranges {:code-scale [0.7 1.5]} :color-keys #{:diff-color}})))))

(defn- lightness [c] (first (model/color->oklch c)))

(deftest built-in-themes
  (doseq [{:keys [id] :as entry} themes/catalog]
    (testing (name id)
      (let [p (themes/params id)]
        (is (= (dissoc p :id :name) (model/normalize-params p))
            "every value valid: nothing dropped")
        (doseq [mode model/modes
                :let [optional #{:surface :bubble}]]
          (is (= (apply disj (set model/palette-keys) optional)
                 (apply disj (set (keys (get p mode))) optional))
              (str mode " gives every color (:surface, :bubble optional)")))
        (is (< (lightness (:bg-dark p)) 0.5) "dark page")
        (is (> (lightness (:bg-light p)) 0.5) "light page")
        (doseq [mode model/modes
                :let [page (model/page-color p mode)
                      fg   (get-in p [mode :fg])]]
          (is (> (Math/abs (- (lightness fg) (lightness page))) 0.4)
              (str mode " text contrasts with its page")))
        (is (number? (:gray-hue p)))
        (is (number? (:accent-chroma p)))
        (is (:name entry))))))
