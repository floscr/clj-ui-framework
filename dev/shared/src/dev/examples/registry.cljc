(ns dev.examples.registry
  "Every shared (stateless, cross-target) example set, in catalogue order.
   Each entry is {:id :title :examples}; :id matches the component sidecar
   id so the site can pair examples with metadata. Target-specific demos
   (dialog, context menu, calendar, lightbox, ...) still live in the target
   mains and will move here as they become target-agnostic."
  (:require [dev.example :as ex]
            [ui.util :as util]
            [dev.examples.button :as button]
            [dev.examples.alert :as alert]
            [dev.examples.badge :as badge]
            [dev.examples.card :as card]
            [dev.examples.accordion :as accordion]
            [dev.examples.table :as table]
            [dev.examples.popover :as popover]
            [dev.examples.command :as command]
            [dev.examples.toolbar :as toolbar]
            [dev.examples.button-group :as button-group]
            [dev.examples.header-patterns :as header-patterns]
            [dev.examples.tabs :as tabs]
            [dev.examples.spinner :as spinner]
            [dev.examples.empty-state :as empty-state]
            [dev.examples.drop-zone :as drop-zone]
            [dev.examples.processing-bar :as processing-bar]
            [dev.examples.toast :as toast]
            [dev.examples.camera :as camera]
            [dev.examples.grid :as grid]
            [dev.examples.skeleton :as skeleton]
            [dev.examples.progress :as progress]
            [dev.examples.switch :as switch]
            [dev.examples.tooltip :as tooltip]
            [dev.examples.breadcrumb :as breadcrumb]
            [dev.examples.separator :as separator]
            [dev.examples.panels :as panels]
            [dev.examples.form :as form]
            [dev.examples.number-field :as number-field]
            [dev.examples.color-picker :as color-picker]
            [dev.examples.chat :as chat]))

(def entries
  [{:id :button          :title "Button"          :examples button/examples}
   {:id :alert           :title "Alert"           :examples alert/examples}
   {:id :badge           :title "Badge"           :examples badge/examples}
   {:id :card            :title "Card"            :examples card/examples}
   {:id :accordion       :title "Accordion"       :examples accordion/examples}
   {:id :table           :title "Table"           :examples table/examples}
   {:id :popover         :title "Popover"         :examples popover/examples}
   {:id :command         :title "Command"         :examples command/examples}
   {:id :toolbar         :title "Toolbar"         :examples toolbar/examples}
   {:id :button-group    :title "Button group"    :examples button-group/examples}
   {:id :header-patterns :title "Header patterns" :examples header-patterns/examples}
   {:id :tabs            :title "Tabs"            :examples tabs/examples}
   {:id :spinner         :title "Spinner"         :examples spinner/examples}
   {:id :empty-state     :title "Empty state"     :examples empty-state/examples}
   {:id :drop-zone       :title "Drop zone"       :examples drop-zone/examples}
   {:id :processing-bar  :title "Processing bar"  :examples processing-bar/examples}
   {:id :toast           :title "Toast"           :examples toast/examples}
   {:id :camera          :title "Camera"          :examples camera/examples}
   {:id :grid            :title "Grid"            :examples grid/examples}
   {:id :skeleton        :title "Skeleton"        :examples skeleton/examples}
   {:id :progress        :title "Progress"        :examples progress/examples}
   {:id :switch          :title "Switch"          :examples switch/examples}
   {:id :tooltip         :title "Tooltip"         :examples tooltip/examples}
   {:id :breadcrumb      :title "Breadcrumb"      :examples breadcrumb/examples}
   {:id :separator       :title "Separator"       :examples separator/examples}
   {:id :panels          :title "Panels"          :examples panels/examples}
   {:id :form            :title "Form"            :examples form/examples}
   {:id :number-field    :title "Number field"    :examples number-field/examples}
   {:id :color-picker    :title "Color picker"    :examples color-picker/examples}
   {:id :chat            :title "Chat"            :examples chat/examples}])

(defn entry
  "The registry entry for a component id (keyword or string)."
  [id]
  (some (fn [e] (when (= (util/kw-name (:id e)) (util/kw-name id)) e)) entries))

(defn section-for
  "Render the component's examples as a titled section on the long demo
   pages (same layout the old per-component demo fns produced)."
  [id]
  (let [{:keys [title examples]} (entry id)]
    (ex/section title (ex/render-examples examples))))
