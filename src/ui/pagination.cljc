(ns ui.pagination
  (:require [clojure.string :as str]))

(defn pagination-item-class-list
  "Generate a vector of CSS class strings for a pagination item."
  [{:keys [active disabled]}]
  (cond-> ["pagination-item"]
    active   (conj "pagination-item--active")
    disabled (conj "pagination-item--disabled")))

(defn pagination-item-classes
  "Generate CSS class string for a pagination item."
  [opts]
  (str/join " " (pagination-item-class-list opts)))

(defn pagination
  "Render a pagination nav.

   Props:
     :current  - current page number (1-based)
     :total    - total number of pages
     :href-fn  - fn of page number → URL string (for :clj)
     :on-click - fn of page number → handler (for :cljs/:squint)
     :class    - additional CSS classes
     :attrs    - additional HTML attributes"
  [{:keys [current total href-fn on-click class attrs] :as _props}]
  (let [pages (range 1 (inc total))
        prev-disabled (= current 1)
        next-disabled (= current total)]
    #?(:squint
       (let [classes (cond-> "pagination" class (str " " class))
             make-item (fn [page label active disabled]
                         [:li {:class (pagination-item-classes {:active active :disabled disabled})}
                          [:a (cond-> {:href (if href-fn (href-fn page) "#")}
                                (and on-click (not disabled))
                                (assoc :on-click (fn [e] (.preventDefault e) (on-click page))))
                           label]])]
         [:nav {:aria-label "Pagination"}
          (into [:ol (merge {:class classes} attrs)]
                (concat
                  [(make-item (dec current) "← Previous" false prev-disabled)]
                  (map (fn [p] (make-item p (str p) (= p current) false)) pages)
                  [(make-item (inc current) "Next →" false next-disabled)]))])

       :cljs
       (let [cls (cond-> ["pagination"] class (conj class))
             make-item (fn [page label active disabled]
                         [:li {:class (pagination-item-class-list {:active active :disabled disabled})}
                          [:a (cond-> {:href (if href-fn (href-fn page) "#")}
                                (and on-click (not disabled))
                                (assoc-in [:on :click] (fn [e] (.preventDefault e) (on-click page))))
                           label]])]
         [:nav {:aria-label "Pagination"}
          (into [:ol (merge {:class cls} attrs)]
                (concat
                  [(make-item (dec current) "← Previous" false prev-disabled)]
                  (map (fn [p] (make-item p (str p) (= p current) false)) pages)
                  [(make-item (inc current) "Next →" false next-disabled)]))])

       :clj
       (let [classes (cond-> "pagination" class (str " " class))
             make-item (fn [page label active disabled]
                         [:li {:class (pagination-item-classes {:active active :disabled disabled})}
                          [:a {:href (if href-fn (href-fn page) "#")} label]])]
         [:nav {:aria-label "Pagination"}
          (into [:ol (merge {:class classes} attrs)]
                (concat
                  [(make-item (dec current) "← Previous" false prev-disabled)]
                  (map (fn [p] (make-item p (str p) (= p current) false)) pages)
                  [(make-item (inc current) "Next →" false next-disabled)]))]))))
