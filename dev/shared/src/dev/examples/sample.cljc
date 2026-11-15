(ns dev.examples.sample
  "Sample data shared by several example sets.")

(def calendar-events
  [{:title "Team standup"     :date "2026-03-29" :time-start "09:00" :time-end "09:30" :color :accent}
   {:title "Lunch with Alex"  :date "2026-03-29" :time-start "12:00" :time-end "13:00" :color :success}
   {:title "Deploy v2.0"      :date "2026-03-29" :time-start "15:00" :color :danger}
   {:title "Design review"    :date "2026-03-30" :time-start "10:00" :time-end "11:30" :color :warning}
   {:title "All-day planning" :date "2026-03-31" :done? true}
   {:title "Sprint retro"     :date "2026-04-01" :time-start "14:00" :time-end "15:00" :color :accent}
   {:title "1:1 with manager" :date "2026-04-02" :time-start "11:00" :time-end "11:30" :color :blue}
   {:title "Release party"    :date "2026-04-03" :time-start "17:00" :color :pink}])

(def files
  [{:name "Documents"            :file-type :folder      :modified "May 10, 2026"}
   {:name "Photos"               :file-type :folder      :modified "May 8, 2026"}
   {:name "project-proposal.pdf" :file-type :document    :size "2.4 MB"  :modified "May 12, 2026"}
   {:name "vacation-photo.jpg"   :file-type :image       :size "4.1 MB"  :modified "May 11, 2026"}
   {:name "budget-2026.xlsx"     :file-type :spreadsheet :size "156 KB"  :modified "May 7, 2026"}
   {:name "intro-video.mp4"      :file-type :video       :size "245 MB"  :modified "May 5, 2026"}
   {:name "podcast-ep12.mp3"     :file-type :audio       :size "48 MB"   :modified "May 3, 2026"}
   {:name "app.clj"              :file-type :code        :size "8.2 KB"  :modified "May 14, 2026"}])

(defn- svg-photo
  "A w × h placeholder photo as an SVG data URI (with a viewBox, so it
   scales like a real image)."
  [w h color label]
  (str "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='" w "' height='" h
       "' viewBox='0 0 " w " " h "'%3E"
       "%3Crect fill='%23" color "' width='" w "' height='" h "'/%3E"
       "%3Ctext x='" (/ w 2) "' y='" (/ h 2) "' text-anchor='middle' dominant-baseline='middle' fill='white' "
       "font-size='" (quot (min w h) 12) "' font-family='sans-serif'%3E" label "%3C/text%3E%3C/svg%3E"))

(defn- svg-tile [color label]
  (svg-photo 400 300 color label))

(def images
  [{:src (svg-tile "4f46e5" "Indigo") :alt "Indigo"}
   {:src (svg-tile "16a34a" "Green")  :alt "Green"}
   {:src (svg-tile "dc2626" "Red")    :alt "Red"}
   {:src (svg-tile "ea580c" "Orange") :alt "Orange"}])

(def photos
  "Gallery photos of mixed aspect, each with a caption (lightbox examples)."
  [{:src (svg-photo 1600 1200 "4f46e5" "Indigo") :alt "Indigo" :width 1600 :height 1200 :caption "Indigo, 4:3"}
   {:src (svg-photo 1000 1500 "16a34a" "Green")  :alt "Green"  :width 1000 :height 1500 :caption "Green, 2:3"}
   {:src (svg-photo 1400 1400 "dc2626" "Red")    :alt "Red"    :width 1400 :height 1400 :caption "Red, square"}
   {:src (svg-photo 1920 1080 "ea580c" "Orange") :alt "Orange" :width 1920 :height 1080 :caption "Orange, 16:9"}
   {:src (svg-photo 1200 1600 "0891b2" "Cyan")   :alt "Cyan"   :width 1200 :height 1600 :caption "Cyan, 3:4"}])
