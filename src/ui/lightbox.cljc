(ns ui.lightbox
  "Lightbox component for fullscreen image viewing.

   Usage (Replicant/ClojureScript):
     (lightbox {:src (:lightbox-image @state)
                :on-close #(swap! state assoc :lightbox-image nil)})

   Only renders when :src is non-nil."
  (:require [ui.icon :as icon]))

(defn lightbox
  "Render a fullscreen image lightbox overlay.

   Props:
     :src      - image src URL (data URI or URL). nil = hidden.
     :alt      - alt text for the image (default: \"image\")
     :on-close - callback to close the lightbox"
  [{:keys [src alt on-close]}]
  (when src
    #?(:cljs
       [:div {:class ["lightbox-overlay"]
              :on {:click (fn [_] (when on-close (on-close)))}}
        [:img {:class ["lightbox-image"]
               :src src
               :alt (or alt "image")
               :on {:click (fn [e] (.stopPropagation e))}}]
        [:button {:class ["lightbox-close"]
                  :on {:click (fn [_] (when on-close (on-close)))}}
         (icon/icon {:icon-name :x :size :md})]]

       :squint
       [:div {:class "lightbox-overlay"
              :on-click (when on-close
                          (fn [e]
                            (when (identical? (.-target e) (.-currentTarget e))
                              (on-close))))}
        [:img {:class "lightbox-image"
               :src src
               :alt (or alt "image")}]
        [:button {:class "lightbox-close"
                  :on-click (when on-close (fn [_] (on-close)))}
         (icon/icon {:icon-name :x :size :md})]]

       :clj
       [:div {:class "lightbox-overlay"}
        [:img {:class "lightbox-image"
               :src src
               :alt (or alt "image")}]
        [:button {:class "lightbox-close"}
         (icon/icon {:icon-name :x :size :md})]])))

(defn image-thumbnail
  "Render a clickable image thumbnail that opens a lightbox on click.

   Props:
     :src      - image src URL
     :alt      - alt text (default: \"image\")
     :class    - additional CSS classes (string or vector)
     :on-click - click handler (typically opens lightbox with this src)"
  [{:keys [src alt on-click] :as props}]
  (let [extra-class (:class props)]
    #?(:cljs
       [:img (cond-> {:class (cond-> ["lightbox-thumb"]
                               extra-class (into (if (string? extra-class) [extra-class] extra-class)))
                      :src src
                      :alt (or alt "image")}
               on-click (assoc :on {:click (fn [_] (on-click))}))]

       :squint
       [:img (cond-> {:class (cond-> "lightbox-thumb"
                               extra-class (str " " (if (string? extra-class) extra-class
                                                        (clojure.string/join " " extra-class))))
                      :src src
                      :alt (or alt "image")}
               on-click (assoc :on-click (fn [_] (on-click))))]

       :clj
       [:img {:class (cond-> "lightbox-thumb"
                       extra-class (str " " (if (string? extra-class) extra-class
                                                (clojure.string/join " " extra-class))))
              :src src
              :alt (or alt "image")}])))
