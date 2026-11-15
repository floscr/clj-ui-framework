(ns ui.lightbox
  "Lightbox: a fullscreen viewer for one image or a gallery.

   Usage (Replicant / Squint, the host owns open and closed):
     (lightbox {:items  [{:src full :thumb small :alt \"…\" :caption \"…\"} …]
                :index  (:open-index @state)
                :on-close  #(swap! state dissoc :open-index)
                :on-change #(swap! state assoc :open-index %)})

   One image keeps the old shape: (lightbox {:src src :on-close close!}).
   Nothing renders while there is no :src / no :items.

   Managed (any target, the only way on :clj): give it an :id and
   :open false, and thumbnails that name it
   (image-thumbnail {:src … :lightbox \"photos\" :index 2}) open it.

   Everything that moves comes from the ui.js.lightbox runtime (bundled
   in ui-runtime.js; squint / replicant SPAs without it add a side-effect
   require of [ui.js.lightbox]): swiping between slides, pinch / wheel /
   double-tap zoom, pull to dismiss, the morph out of and back into the
   thumbnail, keyboard, image loading. It reports back with two
   non-bubbling events on the root, `change` (detail.index) and `close`,
   which :on-change / :on-close listen to."
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.util :as util]
            [ui.video-player :as vp]))

(def default-labels
  {:dialog        "Image viewer"
   :close         "Close"
   :previous      "Previous image"
   :next          "Next image"
   :zoom-in       "Zoom in"
   :zoom-out      "Zoom out"
   :error         "This image could not be loaded."
   :open-original "Open original"})

(defn- class-tokens
  "The caller's :class (a space-joined string or a collection) as tokens."
  [class]
  (cond
    (nil? class)    []
    (string? class) (vec (remove str/blank? (str/split class #"\s+")))
    :else           (vec (remove nil? class))))

(defn- attrs*
  "An attribute map in the target's shape. :class is a vector of tokens
   (nils dropped), :on a map of event name to handler; nil values are
   dropped, so boolean attributes are only ever present or absent."
  [attrs]
  (let [cls  (vec (remove nil? (:class attrs)))
        on   (:on attrs)
        base (reduce-kv (fn [m k v] (if (nil? v) m (assoc m k v)))
                        {}
                        (dissoc attrs :class :on))]
    #?(:squint (cond-> base
                 (seq cls) (assoc :class (str/join " " cls))
                 on        (merge (reduce-kv (fn [m k f]
                                               (if f (assoc m (str "on-" k) f) m))
                                             {} on)))
       :cljs   (cond-> base
                 (seq cls) (assoc :class cls)
                 on        (assoc :on (reduce-kv (fn [m k f] (if f (assoc m k f) m))
                                                 {} on)))
       :clj    (cond-> base
                 (seq cls) (assoc :class (str/join " " cls))))))

(defn- splice?
  "A child that is a sequence of children rather than one element. Squint
   counts strings as sequential, and hiccup elements are vectors."
  [c]
  (and (some? c) (not (string? c)) (not (vector? c)) (sequential? c)))

(defn- el
  "A hiccup element: tag, attribute map (see attrs*), children with nils
   dropped and nested sequences spliced in (squint does not flatten)."
  [tag attrs & children]
  (into [tag (attrs* attrs)]
        (comp (mapcat (fn [c] (if (splice? c) c [c])))
              (remove nil?))
        children))

(defn- slot
  "A slot prop (:actions, :footer) as a vector of elements: it may be one
   hiccup element or a vector / seq of them."
  [x]
  (cond
    (nil? x)                                       []
    (and (vector? x) (or (empty? x) (vector? (first x)))) x
    (vector? x)                                    [x]
    :else                                          (vec x)))

(defn item-type
  "\"image\", \"video\" or \"content\" for a lightbox item."
  [{:keys [type content]}]
  (cond
    type    (util/kw-name type)
    content "content"
    :else   "image"))

(defn item-key
  "The key a thumbnail and its slide share, so the runtime knows which
   thumbnail to morph out of and back into: :key, else :thumb, else :src."
  [{:keys [key thumb src]}]
  (str (or key thumb src)))

(defn- self-event?
  "The event was dispatched on this element, not bubbled up from inside
   (an <input> in custom content fires change too)."
  [e]
  (identical? (.-target e) (.-currentTarget e)))

(defn- icon-button [cls label action icon-name]
  (el :button {:type "button"
               :class (into ["ui-lightbox-button"] cls)
               :aria-label label
               :data-lightbox-action action}
      (icon/icon {:icon-name icon-name :size :md})))

(defn action
  "A round toolbar button for the lightbox's :actions slot.

   Props:
     :icon-name - ui.icon name
     :label     - accessible name
     :href      - renders a link (opens in a new tab) instead of a button
     :download  - with :href, a download link (true or a file name)
     :on-click  - click handler (buttons)"
  [{:keys [icon-name label href download on-click]}]
  (if href
    (el :a {:class ["ui-lightbox-button"]
            :href href
            :target (when-not download "_blank")
            :rel (when-not download "noopener")
            :download (cond (string? download) download download "")
            :aria-label label}
        (icon/icon {:icon-name icon-name :size :md}))
    (el :button {:type "button"
                 :class ["ui-lightbox-button"]
                 :aria-label label
                 :on {:click (when on-click (fn [e] (on-click e)))}}
        (icon/icon {:icon-name icon-name :size :md}))))

(defn- media [labels i item]
  (let [t (item-type item)
        {:keys [src thumb alt width height poster content href]} item
        cls ["ui-lightbox-media" (:class item)]]
    (case t
      "video"
      ;; ui.video-player, sized to the video's aspect by the runtime. It
      ;; renders without a source; the lightbox runtime hands it
      ;; data-lightbox-src when the slide is shown, so a gallery of videos
      ;; doesn't buffer them all at once
      [(el :div {:class ["ui-lightbox-media" "ui-lightbox-player" (:class item)]
                 :data-width (some-> width str)
                 :data-height (some-> height str)
                 :data-autoplay (when (:autoplay item) "")
                 :data-muted (when (:muted item) "")
                 :data-loop (when (:loop item) "")}
           (vp/video-player (merge {:poster (or poster thumb)}
                                   (:player item)
                                   {:src ""
                                    :fit :fill
                                    :attrs (merge (:attrs (:player item))
                                                  {:data-lightbox-src src})})))]

      "content"
      [(el :div {:class ["ui-lightbox-content" (:class item)]} content)]

      ;; image: no src here — the runtime sets it (thumbnail first, then
      ;; the full image once decoded), so a re-render never resets it
      [(el :img {:class cls
                 :data-src src
                 :data-thumb thumb
                 :alt (or alt "")
                 :width width
                 :height height
                 :draggable "false"
                 :decoding "async"})
       (el :div {:class ["ui-lightbox-error"]}
           (el :span {} (:error labels))
           (when-let [url (or href src)]
             (el :a {:href url :target "_blank" :rel "noopener"}
                 (:open-original labels))))])))

(defn- slide [labels i item]
  (apply el :div {:class ["ui-lightbox-slide"]
                  :data-index (str i)
                  :data-type (item-type item)
                  :data-key (item-key item)}
         (concat (media labels i item)
                 [(when-let [caption (:caption item)]
                    (el :div {:class ["ui-lightbox-caption" "ui-lightbox-chrome"]} caption))])))

(defn- thumb-strip [items]
  (el :div {:class ["ui-lightbox-thumbs" "ui-lightbox-chrome"]}
      (map-indexed
       (fn [i item]
         (let [src (or (:thumb item) (:poster item)
                       (when (= "image" (item-type item)) (:src item)))]
           (el :button {:type "button"
                        :class ["ui-lightbox-thumb-button"]
                        :data-lightbox-action "go"
                        :data-index (str i)
                        :aria-label (str (inc i))}
               (if src
                 (el :img {:src src :alt "" :loading "lazy" :draggable "false"})
                 (icon/icon {:icon-name :file :size :sm})))))
       items)))

(defn lightbox
  "Render the lightbox viewer.

   Props:
     :items          - [{:src :thumb :alt :caption :width :height :key
                         :type :poster :content :href :class
                         :autoplay :loop :muted :player} …]
                       :type :image (default), :video, or :content
                       (with :content hiccup, no zoom). A video plays in
                       ui.video-player; :player passes its props through
                       (:title :tracks :segments :buttons :controls
                       :variant :on-select …). It loads only once shown.
     :src / :alt     - shorthand for a single image
     :index          - the item shown (default 0)
     :id             - element id, for managed thumbnails (:lightbox id)
     :open           - false renders it hidden for managed mode (default true)
     :on-close       - called once the close animation has finished
     :on-change      - called with the new index after a swipe / key / button
     :thumbnails     - true shows a thumbnail strip
     :actions        - hiccup for the toolbar, before zoom and close
                       (see `action`)
     :footer         - hiccup for a bar at the bottom (selection, links)
     :labels         - overrides for default-labels
     :history        - true: opening pushes a history entry, Back closes
     :keyboard       - false turns the runtime's keys off
     :scroll-to-zoom - true: a plain mouse wheel zooms (default: pans when
                       zoomed, ctrl / pinch zooms)
     :max-zoom       - zoom cap (default: the image's own resolution, 2× to 8×)
     :contained      - true: a preview inside its parent (absolute, no
                       page scroll lock, no keys)
     :class          - extra classes on the root"
  [{:keys [items src alt index id open on-close on-change thumbnails actions
           footer labels history keyboard scroll-to-zoom max-zoom contained
           class]}]
  (let [items  (vec (or (seq items) (when src [{:src src :alt (or alt "image")}])))
        n      (count items)
        index  (min (max 0 (or index 0)) (max 0 (dec n)))
        labels (merge default-labels labels)]
    (when (pos? n)
      (el :div {:class (into ["ui-lightbox" (when contained "ui-lightbox--contained")]
                             (class-tokens class))
                :id id
                :hidden (when (false? open) true)
                :data-lightbox ""
                :data-index (str index)
                :data-count (str n)
                :data-thumbnails (when thumbnails "")
                :data-history (when history "")
                :data-keyboard (when (false? keyboard) "off")
                :data-wheel-zoom (when scroll-to-zoom "")
                :data-max-zoom (some-> max-zoom str)
                :data-contained (when contained "")
                :role "dialog"
                :aria-modal "true"
                :aria-label (:dialog labels)
                :on {:close  (when on-close
                               (fn [e] (when (self-event? e) (on-close))))
                     :change (when on-change
                               (fn [e] (when (self-event? e)
                                         (on-change (.. e -detail -index)))))}}
          (el :div {:class ["ui-lightbox-backdrop"]})
          (el :div {:class ["ui-lightbox-slides"]}
              (map-indexed (fn [i item] (slide labels i item)) items))
          (el :div {:class ["ui-lightbox-top" "ui-lightbox-chrome"]}
              (el :span {:class ["ui-lightbox-counter"]} (str (inc index) " / " n))
              (apply el :div {:class ["ui-lightbox-actions"]}
                     (concat (slot actions)
                             [(icon-button ["ui-lightbox-zoom-button"] (:zoom-out labels) "zoom-out" :minus)
                              (icon-button ["ui-lightbox-zoom-button"] (:zoom-in labels) "zoom-in" :plus)
                              (icon-button [] (:close labels) "close" :x)])))
          (icon-button ["ui-lightbox-nav" "ui-lightbox-prev" "ui-lightbox-chrome"]
                       (:previous labels) "prev" :chevron-left)
          (icon-button ["ui-lightbox-nav" "ui-lightbox-next" "ui-lightbox-chrome"]
                       (:next labels) "next" :chevron-right)
          (when footer
            (apply el :div {:class ["ui-lightbox-footer" "ui-lightbox-chrome"]} (slot footer)))
          (when thumbnails
            (thumb-strip items))))))

(defn image-thumbnail
  "A clickable image that opens a lightbox, and the place the lightbox
   morphs out of and back into.

   Props:
     :src          - image src URL
     :alt          - alt text (default: \"image\")
     :class        - additional CSS classes (string or vector)
     :on-click     - click handler (typically opens the lightbox)
     :lightbox     - id of a managed lightbox to open (no :on-click needed)
     :index        - the item it opens, with :lightbox
     :lightbox-key - the item key it stands for (default :src; see item-key)"
  [{:keys [src alt class on-click lightbox index lightbox-key]}]
  (el :img {:class (into ["lightbox-thumb"] (class-tokens class))
            :src src
            :alt (or alt "image")
            :data-lightbox-key (str (or lightbox-key src))
            :data-lightbox-open lightbox
            :data-lightbox-index (some-> index str)
            :on {:click (when on-click (fn [_] (on-click)))}}))
