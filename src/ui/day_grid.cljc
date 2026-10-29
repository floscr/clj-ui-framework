(ns ui.day-grid
  "A week-by-week grid of days, one square per day, colored by what happened
   on it: done, missed, excused, a rest day, a day still to come. The record
   of a habit or a countdown under its streak numbers."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def colors
  "Fill colors for :done days. Each has a .day-grid-<name> class."
  #{"accent" "success" "teal" "blue" "orange" "pink"})

(def states
  "Day states with their own look. Any other state still gets a
   day-grid-<state> class, so an app can style it."
  #{"done" "missed" "excused" "rest" "today" "future"})

(def default-labels
  "Monday-first weekday initials for the header row."
  ["M" "T" "W" "T" "F" "S" "S"])

(defn day-grid-class-list
  "Classes for the grid. :color picks the fill of done days (default
   success)."
  [{:keys [color]}]
  (let [c (some-> color util/kw-name)]
    (cond-> ["day-grid"]
      (contains? colors c) (conj (str "day-grid-" c)))))

(defn day-grid-classes
  "Space-joined form of day-grid-class-list."
  [opts]
  (str/join " " (day-grid-class-list opts)))

(defn day-cell-class-list
  "Classes for one day: its state, plus today when :today is set."
  [{:keys [state today]}]
  (let [s (some-> state util/kw-name)]
    (cond-> ["day-grid-cell"]
      s     (conj (str "day-grid-" s))
      today (conj "day-grid-today"))))

(defn day-cell-classes
  "Space-joined form of day-cell-class-list."
  [day]
  (str/join " " (day-cell-class-list day)))

(defn day-grid
  "Days as squares in rows of seven under a weekday header.

   Props:
     :days   - day maps in order, the first on the first column (pad the
               first week yourself so it starts on the right weekday):
                 :date  - shown as the tooltip when :title is missing
                 :state - :done | :missed | :excused | :rest | :today |
                          :future, or nil for a plain empty day
                 :today - boolean; rings the day
                 :title - tooltip and accessible name
     :color  - fill of done days: :success (default), :accent, :teal,
               :blue, :orange or :pink
     :labels - header labels (default M T W T F S S); false for no header
     :class  - additional CSS classes
     :attrs  - additional HTML attributes map"
  [{:keys [days color labels class attrs] :as _props}]
  (let [labels (if (nil? labels) default-labels labels)]
    (-> [:div (util/class-attrs (day-grid-class-list {:color color}) class attrs)]
        (into (map (fn [l] [:span {:class "day-grid-label" :aria-hidden "true"} l])
                   (when labels labels)))
        (into (map (fn [{:keys [date title] :as day}]
                     (let [t (or title date)]
                       [:span (util/class-attrs (day-cell-class-list day) nil
                                                (when t {:title t :role "img" :aria-label t}))]))
                   days)))))
