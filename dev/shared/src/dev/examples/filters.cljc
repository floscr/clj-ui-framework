(ns dev.examples.filters
  (:require [dev.example :refer [example column log-click]]
            [ui.filters :as filters]
            [ui.form :as form]
            [ui.chip :as chip]
            [ui.icon :as icon]
            [ui.select :as select]
            [ui.button :as button]))

(def ^:private body-lengths
  [{:value "sentence" :label "1 sentence"}
   {:value "paragraph" :label "1 paragraph"}
   {:value "all" :label "Everything"}])

(def examples
  [(example :table "A table's view: presets, rules, columns and display options"
     (fn []
       (filters/filters {}
         (filters/filter-band {}
           (filters/preset-bar {}
             (filters/preset {:on-click (log-click "Most played")} "Most played")
             (filters/preset {:on-click (log-click "Newest")} "Newest")
             (filters/preset {:active true :dirty true
                              :on-click (log-click "JQ") :on-save (log-click "save")
                              :on-revert (log-click "revert") :save-attrs {} :revert-attrs {}}
               "JQ")
             (filters/preset {:on-click (log-click "Mixtapes")} "Mixtapes")
             (filters/preset-add {:on-click (log-click "new preset")}))
           (filters/filter-spacer)
           (button/button {:variant :ghost :size :sm :icon :trash
                           :attrs {:title "Delete preset" :aria-label "Delete preset"}}))
         (filters/filter-band {:label "Find"}
           (form/form-input {:type :search :icon-left :search :placeholder "Title, artist, album…"})
           (chip/chip {:active true} (icon/icon {:icon-name :heart :size :sm}) "Hearted")
           (chip/chip {} "Unplayed")
           (chip/chip {} (icon/icon {:icon-name :download :size :sm}) "Downloaded"))
         (filters/filter-band {:label "Rules"}
           (filters/rule-list {}
             (filters/rule {:verb "Sort by" :icon :arrow-up-down :toggle false :remove-attrs {}}
               (filters/rule-value {} "Played")
               (filters/rule-value {:icon :arrow-down} "Newest first"))
             (filters/rule {:verb "Then by" :icon :arrow-up-down :toggle false :remove-attrs {}}
               (filters/rule-value {} "Artist")
               (filters/rule-value {:icon :arrow-up} "A to Z"))
             (filters/rule {:verb "Hide" :icon :eye-off :on-verb (log-click "hide or keep") :verb-attrs {}
                            :remove-attrs {}}
               "where"
               (filters/rule-value {} "Title")
               (filters/rule-value {} "contains")
               (filters/rule-text {:value "live" :placeholder "Text"}))
             (filters/rule-add {:hint "Sort, hide, keep only" :on-click (log-click "add rule")})))
         (filters/filter-band {:label "Columns"}
           (filters/filter-toggle {:reorder true :on true} "Title")
           (filters/filter-toggle {:reorder true :on true} "Artist")
           (filters/filter-toggle {:reorder true :on true} "Added")
           (filters/filter-toggle {:reorder true} "Plays")
           (filters/filter-toggle {:reorder true :on true} "Playlist")
           (filters/filter-divider)
           (filters/filter-add {} "Album")
           (filters/filter-add {} "Duration"))
         (filters/filter-band {:label "Show"}
           (filters/filter-toggle {:icon :music} "Icons")
           (filters/filter-toggle {:icon :image :on true} "Covers"))
         (filters/filter-footer {}
           "Own view for “Recently added”"
           (filters/filter-action {:disabled true} "Update")
           (filters/filter-action {} "Use shared view")
           (filters/filter-spacer)
           (filters/filter-action {} "Clear rules")
           (filters/filter-action {} "Reset columns")))))

   (example :list "A list's view: a paused rule, an option that takes a value"
     (fn []
       (filters/filters {}
         (filters/filter-band {}
           (filters/preset-bar {}
             (filters/preset {:active true} "Agenda")
             (filters/preset {} "By state")
             (filters/preset {} "Newest")
             (filters/preset-add {})))
         (filters/filter-band {:label "Find"}
           (form/form-input {:type :search :icon-left :search :placeholder "Filter inbox or :tag…"}))
         (filters/filter-band {:label "Rules"}
           (filters/rule-list {}
             (filters/rule {:verb "Sort by" :icon :arrow-up-down :remove-attrs {}}
               (filters/rule-value {} "State")
               (filters/rule-value {:icon :arrow-up} "Ascending"))
             (filters/rule {:verb "Group by" :icon :layers :remove-attrs {}}
               (filters/rule-value {} "Agenda")
               "ordered"
               (filters/rule-value {} "Default"))
             (filters/rule {:verb "Hide" :icon :eye-off :off true :remove-attrs {}}
               (filters/rule-value {} "Older")
               "than"
               (filters/rule-value {} "1 week"))
             (filters/rule {:verb "Keep only" :icon :filter :remove-attrs {}}
               (filters/rule-value {} "Todo state")
               "is"
               (filters/rule-value {} "Open"))
             (filters/rule-add {:hint "Sort, group, hide, keep only"})))
         (filters/filter-band {:label "Show"}
           (filters/filter-toggle {:icon :circle-check :on true} "State")
           (filters/filter-toggle {:icon :calendar :on true} "Date")
           (filters/filter-toggle {:icon :bookmark :on true} "Tags")
           (filters/filter-toggle {:icon :image} "Thumbnail")
           (filters/filter-toggle {:icon :file-text :on true
                                   :value (select/select {:options body-lengths :value "sentence"})}
             "Body text")
           (filters/filter-toggle {:icon :link} "Links")
           (filters/filter-toggle {:icon :list} "Subtasks")))))

   (example :disclosure "A filter that stays out of the way: its summary opens the choices"
     (fn []
       (filters/filter-disclosure {:summary "15+ comments · unseen only · most comments" :open true}
         (filters/filters {:bare true :dense true}
           (filters/filter-band {:label "Comments"}
             (filters/filter-toggle {} "Any")
             (filters/filter-toggle {:on true} "15+")
             (filters/filter-toggle {} "50+")
             (filters/filter-toggle {} "100+"))
           (filters/filter-band {:label "Order"}
             (filters/filter-toggle {} "HN rank")
             (filters/filter-toggle {:on true} "Most comments")
             (filters/filter-toggle {} "Newest"))
           (filters/filter-band {:label "Seen"}
             (filters/filter-toggle {} "Faded")
             (filters/filter-toggle {:on true} "Hidden"))))))

   (example :presets "Preset bar: clean, changed, a view nobody saved yet"
     (fn []
       (column
         (filters/preset-bar {}
           (filters/preset {:active true} "Newest")
           (filters/preset {} "Hearted")
           (filters/preset-add {}))
         (filters/preset-bar {}
           (filters/preset {:active true :dirty true :save-attrs {} :revert-attrs {}} "Newest")
           (filters/preset {} "Hearted")
           (filters/preset-add {}))
         (filters/preset-bar {}
           (filters/preset {} "Newest")
           (filters/preset {} "Hearted")
           (filters/preset-add {} "Save as preset")))))

   (example :empty "No rules yet"
     (fn []
       (filters/rule-list {}
         (filters/rule-empty {} "No rules. The list shows in its own order.")
         (filters/rule-add {:hint "Sort, hide, keep only"}))))])
