(ns dev.examples.form
  (:require [dev.example :refer [example sx]]
            [ui.form :as form]
            [ui.button :as button]))

(def examples
  [(example :fields "Fields"
     (fn []
       [:form {:style (sx {:max-width "480px"})}
        (form/form-field {:label "Name"}
          (form/form-input {:type :text :placeholder "Enter your name"}))
        (form/form-field {:label "Email"}
          (form/form-input {:type :email :placeholder "you@example.com"}))
        (form/form-field {:label "Password" :hint "At least 8 characters"}
          (form/form-input {:type :password :placeholder "Password"}))
        (form/form-field {:label "Select"}
          (form/form-select {:placeholder "Select an option"
                             :options [{:value "a" :label "Option A"}
                                       {:value "b" :label "Option B"}
                                       {:value "c" :label "Option C"}]}))
        (form/form-field {:label "Message"}
          (form/form-textarea {:placeholder "Your message…"}))
        (form/form-field {:label "Auto-growing (3 lines max)"}
          (form/form-textarea-auto {:placeholder "Grows as you type…" :max-rows 3}))
        (form/form-field {:label "Disabled"}
          (form/form-input {:type :text :placeholder "Disabled" :disabled true}))
        (form/form-field {:label "File"}
          (form/form-file {}))
        (form/form-field {:label "Date and time"}
          (form/form-input {:type :datetime-local}))
        (form/form-field {:label "Date"}
          (form/form-input {:type :date}))
        (form/form-field {:label "Search (icon left)"}
          (form/form-input {:type :text :placeholder "Search…" :icon-left :search}))
        (form/form-field {:label "URL (both icons)"}
          (form/form-input {:type :text :placeholder "example.com" :icon-left :globe :icon-right :check}))
        (form/form-checkbox {:label "I agree to the terms"})
        (form/form-radio-group {:label "Preference"
                                :radio-name "pref"
                                :options [{:value "a" :label "Option A"}
                                          {:value "b" :label "Option B"}
                                          {:value "c" :label "Option C"}]})
        (form/form-field {:label "Volume"}
          (form/form-range {:min 0 :max 100 :value 50}))
        (button/button {:variant :primary :attrs {:type "submit"}} "Submit")]))

   (example :group "Input group"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (form/form-group {}
          (form/form-group-addon {} "https://")
          (form/form-input {:placeholder "subdomain"})
          (button/button {:variant :primary :size :sm} "Go"))]))

   (example :search-bar "Search bar preset"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (form/search-bar {:placeholder "Search…"})]))

   (example :error "Validation error"
     (fn []
       [:div {:style (sx {:max-width "480px"})}
        (form/form-field {:label "Email" :error "Enter an email like name@company.com."}
          (form/form-input {:type :email :error true :value "invalid-email"}))]))])
