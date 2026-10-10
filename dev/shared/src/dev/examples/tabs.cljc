(ns dev.examples.tabs
  (:require [dev.example :refer [example cls sx]]
            [ui.tabs :as tabs]))

(defn- para [txt]
  [:p {:class (cls "text-sm" "text-muted") :style (sx {:margin "0"})} txt])

(def examples
  [(example :boxed "Boxed (segmented)"
     (fn []
       (tabs/tabs
        {:variant :boxed
         :default "account"
         :tabs [{:id "account"  :label "Account"  :content (para "Make changes to your account here.")}
                {:id "password" :label "Password" :content (para "Change your password here.")}
                {:id "settings" :label "Settings" :content (para "Manage your preferences.")}]})))

   (example :line "Line (underlined)"
     (fn []
       (tabs/tabs
        {:variant :line
         :default "overview"
         :tabs [{:id "overview"  :label "Overview"  :content (para "A high-level summary of the project.")}
                {:id "analytics" :label "Analytics" :content (para "Traffic and engagement metrics.")}
                {:id "reports"   :label "Reports"   :content (para "Downloadable reports and exports.")}]})))])
