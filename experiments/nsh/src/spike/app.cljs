(ns spike.app
  "Spike: the framework's real ui.button (its :cljs/Replicant branch) rendered
   as native NSH views. Whole-subtree re-render on a signal change; no VDOM."
  (:require ["nsh" :refer [mount signal]]
            ["nsh:rt" :refer [element prop insert]]
            [spike.render :as r]
            [ui.button :as button]))

(def clicks (signal 0))

(defn view [n]
  [:div {:class "gap-4"}
   [:span {:class "text-xl font-medium text-zinc-900"} (str "Clicked " n " times")]
   [:div {:class "flex-row flex-wrap gap-2"}
    (button/button {:variant :primary :on-click (fn [_] (.update clicks inc))} "Add one")
    (button/button {:variant :secondary :size :sm :on-click (fn [_] (.set clicks 0))} "Reset")
    (button/button {:variant :danger :disabled true} "Disabled")]
   (button/button {:variant :primary :loading true} "Loading")
   [:span {:class "text-sm text-zinc-500"}
    (str "hiccup from ui.button: " (pr-str (button/button-class-list {:variant :primary})))]])

(defn app []
  (let [root (element "view")]
    (prop root "style" #js {:flex 1 :padding 24 :gap 16 :backgroundColor "#ffffff"})
    (insert root (fn [] (r/render (view (clicks)))))
    root))

(mount app)
