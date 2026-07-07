(ns ui.icon
  (:require [clojure.string :as str]
            [ui.util :as util]))

;; ── Icon path data ──────────────────────────────────────────────────
;; All icons use 24×24 viewBox, stroke-based (Lucide-compatible).
;; Each entry is a vector of SVG child elements as hiccup.

(def icon-paths
  {;; ── Navigation ──────────────────────────────────────────────────
   :home
   [[:path {:d "M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"}]
    [:path {:d "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"}]]

   :menu
   [[:path {:d "M4 5h16"}]
    [:path {:d "M4 12h16"}]
    [:path {:d "M4 19h16"}]]

   :x
   [[:path {:d "M18 6 6 18"}]
    [:path {:d "m6 6 12 12"}]]

   :chevron-down  [[:path {:d "m6 9 6 6 6-6"}]]
   :chevron-up    [[:path {:d "m18 15-6-6-6 6"}]]
   :chevron-left  [[:path {:d "m15 18-6-6 6-6"}]]
   :chevron-right [[:path {:d "m9 18 6-6-6-6"}]]

   :arrow-left
   [[:path {:d "m12 19-7-7 7-7"}]
    [:path {:d "M19 12H5"}]]

   :arrow-right
   [[:path {:d "M5 12h14"}]
    [:path {:d "m12 5 7 7-7 7"}]]

   :arrow-up
   [[:path {:d "m5 12 7-7 7 7"}]
    [:path {:d "M12 19V5"}]]

   :arrow-down
   [[:path {:d "M12 5v14"}]
    [:path {:d "m19 12-7 7-7-7"}]]

   :external-link
   [[:path {:d "M15 3h6v6"}]
    [:path {:d "M10 14 21 3"}]
    [:path {:d "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"}]]

   ;; ── Actions ─────────────────────────────────────────────────────
   :search
   [[:circle {:cx "11" :cy "11" :r "8"}]
    [:path {:d "m21 21-4.34-4.34"}]]

   :plus
   [[:path {:d "M5 12h14"}]
    [:path {:d "M12 5v14"}]]

   :minus
   [[:path {:d "M5 12h14"}]]

   :check
   [[:path {:d "M20 6 9 17l-5-5"}]]

   :edit
   [[:path {:d "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z"}]]

   :trash
   [[:path {:d "M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"}]
    [:path {:d "M3 6h18"}]
    [:path {:d "M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"}]]

   :download
   [[:path {:d "M12 15V3"}]
    [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
    [:path {:d "m7 10 5 5 5-5"}]]

   :upload
   [[:path {:d "M12 3v12"}]
    [:path {:d "m17 8-5-5-5 5"}]
    [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]]

   :copy
   [[:rect {:width "14" :height "14" :x "8" :y "8" :rx "2" :ry "2"}]
    [:path {:d "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"}]]

   :filter
   [[:polygon {:points "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"}]]

   :link
   [[:path {:d "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"}]
    [:path {:d "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"}]]

   :refresh
   [[:path {:d "M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"}]
    [:path {:d "M3 3v5h5"}]
    [:path {:d "M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"}]
    [:path {:d "M16 16h5v5"}]]

   ;; ── Objects ─────────────────────────────────────────────────────
   :file
   [[:path {:d "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"}]
    [:path {:d "M14 2v5a1 1 0 0 0 1 1h5"}]]

   :file-text
   [[:path {:d "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"}]
    [:path {:d "M14 2v5a1 1 0 0 0 1 1h5"}]
    [:path {:d "M16 13H8"}]
    [:path {:d "M16 17H8"}]
    [:path {:d "M10 9H8"}]]

   :file-spreadsheet
   [[:path {:d "M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"}]
    [:path {:d "M14 2v5a1 1 0 0 0 1 1h5"}]
    [:path {:d "M8 13h2"}]
    [:path {:d "M14 13h2"}]
    [:path {:d "M8 17h2"}]
    [:path {:d "M14 17h2"}]]

   :folder
   [[:path {:d "M20 20a2 2 0 0 0 2-2V8a2 2 0 0 0-2-2h-7.9a2 2 0 0 1-1.69-.9L9.6 3.9A2 2 0 0 0 7.93 3H4a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2Z"}]]

   :image
   [[:rect {:width "18" :height "18" :x "3" :y "3" :rx "2" :ry "2"}]
    [:circle {:cx "9" :cy "9" :r "2"}]
    [:path {:d "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"}]]

   :mail
   [[:rect {:x "2" :y "4" :width "20" :height "16" :rx "2"}]
    [:path {:d "m22 7-8.991 5.727a2 2 0 0 1-2.009 0L2 7"}]]

   :bell
   [[:path {:d "M10.268 21a2 2 0 0 0 3.464 0"}]
    [:path {:d "M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"}]]

   :calendar
   [[:rect {:width "18" :height "18" :x "3" :y "4" :rx "2"}]
    [:path {:d "M16 2v4"}]
    [:path {:d "M8 2v4"}]
    [:path {:d "M3 10h18"}]]

   :clock
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M12 6v6l4 2"}]]

   :bookmark
   [[:path {:d "M17 3a2 2 0 0 1 2 2v15a1 1 0 0 1-1.496.868l-4.512-2.578a2 2 0 0 0-1.984 0l-4.512 2.578A1 1 0 0 1 5 20V5a2 2 0 0 1 2-2z"}]]

   :star
   [[:path {:d "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"}]]

   :heart
   [[:path {:d "M2 9.5a5.5 5.5 0 0 1 9.591-3.676.56.56 0 0 0 .818 0A5.49 5.49 0 0 1 22 9.5c0 2.29-1.5 4-3 5.5l-5.492 5.313a2 2 0 0 1-3 .019L5 15c-1.5-1.5-3-3.2-3-5.5"}]]

   :inbox
   [[:polyline {:points "22 12 16 12 14 15 10 15 8 12 2 12"}]
    [:path {:d "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"}]]

   :layers
   [[:path {:d "M12.83 2.18a2 2 0 0 0-1.66 0L2.6 6.08a1 1 0 0 0 0 1.83l8.58 3.91a2 2 0 0 0 1.66 0l8.58-3.9a1 1 0 0 0 0-1.83z"}]
    [:path {:d "M2 12a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 12"}]
    [:path {:d "M2 17a1 1 0 0 0 .58.91l8.6 3.91a2 2 0 0 0 1.65 0l8.58-3.9A1 1 0 0 0 22 17"}]]

   :package
   [[:path {:d "M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z"}]
    [:path {:d "M12 22V12"}]
    [:polyline {:points "3.29 7 12 12 20.71 7"}]
    [:path {:d "m7.5 4.27 9 5.15"}]]

   ;; ── UI / System ─────────────────────────────────────────────────
   :settings
   [[:path {:d "M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915"}]
    [:circle {:cx "12" :cy "12" :r "3"}]]

   :user
   [[:path {:d "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"}]
    [:circle {:cx "12" :cy "7" :r "4"}]]

   :users
   [[:path {:d "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"}]
    [:circle {:cx "9" :cy "7" :r "4"}]
    [:path {:d "M16 3.128a4 4 0 0 1 0 7.744"}]
    [:path {:d "M22 21v-2a4 4 0 0 0-3-3.87"}]]

   :log-out
   [[:path {:d "m16 17 5-5-5-5"}]
    [:path {:d "M21 12H9"}]
    [:path {:d "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"}]]

   :log-in
   [[:path {:d "m10 17 5-5-5-5"}]
    [:path {:d "M15 12H3"}]
    [:path {:d "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"}]]

   :eye
   [[:path {:d "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0"}]
    [:circle {:cx "12" :cy "12" :r "3"}]]

   :eye-off
   [[:path {:d "M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49"}]
    [:path {:d "M14.084 14.158a3 3 0 0 1-4.242-4.242"}]
    [:path {:d "M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143"}]
    [:path {:d "m2 2 20 20"}]]

   :lock
   [[:rect {:width "18" :height "11" :x "3" :y "11" :rx "2" :ry "2"}]
    [:path {:d "M7 11V7a5 5 0 0 1 10 0v4"}]]

   :grid
   [[:rect {:x "3" :y "3" :width "18" :height "18" :rx "2"}]
    [:path {:d "M3 9h18"}]
    [:path {:d "M3 15h18"}]
    [:path {:d "M9 3v18"}]
    [:path {:d "M15 3v18"}]]

   :list
   [[:path {:d "M3 5h.01"}]
    [:path {:d "M3 12h.01"}]
    [:path {:d "M3 19h.01"}]
    [:path {:d "M8 5h13"}]
    [:path {:d "M8 12h13"}]
    [:path {:d "M8 19h13"}]]

   :layout-dashboard
   [[:rect {:width "7" :height "9" :x "3" :y "3" :rx "1"}]
    [:rect {:width "7" :height "5" :x "14" :y "3" :rx "1"}]
    [:rect {:width "7" :height "9" :x "14" :y "12" :rx "1"}]
    [:rect {:width "7" :height "5" :x "3" :y "16" :rx "1"}]]

   :monitor
   [[:rect {:width "20" :height "14" :x "2" :y "3" :rx "2"}]
    [:line {:x1 "8" :x2 "16" :y1 "21" :y2 "21"}]
    [:line {:x1 "12" :x2 "12" :y1 "17" :y2 "21"}]]

   :moon
   [[:path {:d "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"}]]

   :sun
   [[:circle {:cx "12" :cy "12" :r "4"}]
    [:path {:d "M12 2v2"}]
    [:path {:d "M12 20v2"}]
    [:path {:d "m4.93 4.93 1.41 1.41"}]
    [:path {:d "m17.66 17.66 1.41 1.41"}]
    [:path {:d "M2 12h2"}]
    [:path {:d "M20 12h2"}]
    [:path {:d "m6.34 17.66-1.41 1.41"}]
    [:path {:d "m19.07 4.93-1.41 1.41"}]]

   ;; ── Status ──────────────────────────────────────────────────────
   :alert-triangle
   [[:path {:d "m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3"}]
    [:path {:d "M12 9v4"}]
    [:path {:d "M12 17h.01"}]]

   :alert-circle
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:line {:x1 "12" :x2 "12" :y1 "8" :y2 "12"}]
    [:line {:x1 "12" :x2 "12.01" :y1 "16" :y2 "16"}]]

   :info
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M12 16v-4"}]
    [:path {:d "M12 8h.01"}]]

   :circle-check
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "m9 12 2 2 4-4"}]]

   :circle-x
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "m15 9-6 6"}]
    [:path {:d "m9 9 6 6"}]]

   ;; ── Media ────────────────────────────────────────────────────────
   :play
   [[:path {:d "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"}]]

   :pause
   [[:rect {:x "14" :y "4" :width "4" :height "16" :rx "1"}]
    [:rect {:x "6" :y "4" :width "4" :height "16" :rx "1"}]]

   :skip-back
   [[:path {:d "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z"}]
    [:path {:d "M3 20V4"}]]

   :skip-forward
   [[:path {:d "M21 4v16"}]
    [:path {:d "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z"}]]

   :shuffle
   [[:path {:d "m18 14 4 4-4 4"}]
    [:path {:d "m18 2 4 4-4 4"}]
    [:path {:d "M2 18h1.973a4 4 0 0 0 3.3-1.7l5.454-8.6a4 4 0 0 1 3.3-1.7H22"}]
    [:path {:d "M2 6h1.972a4 4 0 0 1 3.6 2.2"}]
    [:path {:d "M22 18h-6.041a4 4 0 0 1-3.3-1.8l-.359-.45"}]]

   :repeat
   [[:path {:d "m17 2 4 4-4 4"}]
    [:path {:d "M3 11v-1a4 4 0 0 1 4-4h14"}]
    [:path {:d "m7 22-4-4 4-4"}]
    [:path {:d "M21 13v1a4 4 0 0 1-4 4H3"}]]

   :volume-2
   [[:path {:d "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z"}]
    [:path {:d "M16 9a5 5 0 0 1 0 6"}]
    [:path {:d "M19.364 18.364a9 9 0 0 0 0-12.728"}]]

   :music
   [[:path {:d "M9 18V5l12-2v13"}]
    [:circle {:cx "6" :cy "18" :r "3" :fill "currentColor"}]
    [:circle {:cx "18" :cy "16" :r "3" :fill "currentColor"}]]

   :film
   [[:rect {:width "18" :height "18" :x "3" :y "3" :rx "2"}]
    [:path {:d "M7 3v18"}]
    [:path {:d "M3 7.5h4"}]
    [:path {:d "M3 12h18"}]
    [:path {:d "M3 16.5h4"}]
    [:path {:d "M17 3v18"}]
    [:path {:d "M17 7.5h4"}]
    [:path {:d "M17 16.5h4"}]]

   :mic
   [[:path {:d "M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"}]
    [:path {:d "M19 10v2a7 7 0 0 1-14 0v-2"}]
    [:path {:d "M12 19v3"}]]

   ;; ── Dev / Technical ─────────────────────────────────────────────
   :code
   [[:path {:d "m16 18 6-6-6-6"}]
    [:path {:d "m8 6-6 6 6 6"}]]

   :terminal
   [[:path {:d "m4 17 6-6-6-6"}]
    [:path {:d "M12 19h8"}]]

   :database
   [[:ellipse {:cx "12" :cy "5" :rx "9" :ry "3"}]
    [:path {:d "M3 5V19A9 3 0 0 0 21 19V5"}]
    [:path {:d "M3 12A9 3 0 0 0 21 12"}]]

   :globe
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M12 2a14.5 14.5 0 0 0 0 20 14.5 14.5 0 0 0 0-20"}]
    [:path {:d "M2 12h20"}]]

   :shield
   [[:path {:d "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"}]]

   :zap
   [[:path {:d "M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"}]]

   :book-open
   [[:path {:d "M12 7v14"}]
    [:path {:d "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"}]]

   :map-pin
   [[:path {:d "M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0"}]
    [:circle {:cx "12" :cy "10" :r "3"}]]})

;; ── Filled icon variants ────────────────────────────────────────────
;; Used when :filled true is passed to the icon component.
;; Lines/strokes that must remain visible override with per-element attrs.

(def filled-icon-paths
  {:play
   [[:path {:d "M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z"}]]

   :pause
   [[:rect {:x "14" :y "4" :width "4" :height "16" :rx "1"}]
    [:rect {:x "6" :y "4" :width "4" :height "16" :rx "1"}]]

   :skip-back
   [[:path {:d "M17.971 4.285A2 2 0 0 1 21 6v12a2 2 0 0 1-3.029 1.715l-9.997-5.998a2 2 0 0 1-.003-3.432z"}]
    [:rect {:x "2" :y "4" :width "2" :height "16" :rx "1"}]]

   :skip-forward
   [[:rect {:x "20" :y "4" :width "2" :height "16" :rx "1"}]
    [:path {:d "M6.029 4.285A2 2 0 0 0 3 6v12a2 2 0 0 0 3.029 1.715l9.997-5.998a2 2 0 0 0 .003-3.432z"}]]

   :repeat
   [[:path {:d "m17 2 4 4-4 4z"}]
    [:path {:d "M3 11v-1a4 4 0 0 1 4-4h14" :fill "none" :stroke "currentColor" :stroke-width "2"}]
    [:path {:d "m7 22-4-4 4-4z"}]
    [:path {:d "M21 13v1a4 4 0 0 1-4 4H3" :fill "none" :stroke "currentColor" :stroke-width "2"}]]

   :volume-2
   [[:path {:d "M11 4.702a.705.705 0 0 0-1.203-.498L6.413 7.587A1.4 1.4 0 0 1 5.416 8H3a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h2.416a1.4 1.4 0 0 1 .997.413l3.383 3.384A.705.705 0 0 0 11 19.298z"}]
    [:path {:d "M16 9a5 5 0 0 1 0 6" :fill "none" :stroke "currentColor" :stroke-width "2"}]
    [:path {:d "M19.364 18.364a9 9 0 0 0 0-12.728" :fill "none" :stroke "currentColor" :stroke-width "2"}]]})

;; ── Public API ──────────────────────────────────────────────────────

(def icon-names
  "Set of all available icon name keywords."
  (set (keys icon-paths)))

(def default-size "md")

(defn icon-class-list
  "Returns a vector of CSS class strings for an icon.
   Sizes: :sm (16px), :md (20px), :lg (24px), :xl (32px)."
  [{:keys [size]}]
  (let [s (or (some-> size util/kw-name) default-size)]
    (cond-> ["icon"]
      (not= s "md") (conj (str "icon-" s)))))

(defn icon-classes
  "Returns a space-joined class string for an icon."
  [opts]
  (str/join " " (icon-class-list opts)))

(defn icon
  "Render an inline SVG icon.

   Props:
     :icon-name - icon name keyword (e.g. :home, :search, :settings)
     :size      - :sm, :md (default), :lg, :xl
     :class     - additional CSS classes
     :attrs     - additional HTML/SVG attributes
     :filled    - boolean, render filled variant (media icons)

   Returns hiccup SVG element. Returns nil for unknown icon names."
  [{:keys [icon-name size class attrs filled] :as _props}]
  (let [n (util/kw-name icon-name)
        kw #?(:squint n :cljs (keyword n) :clj (keyword n))
        paths (if filled
                (or (get filled-icon-paths kw) (get icon-paths kw))
                (get icon-paths kw))]
    (when paths
      #?(:squint
         (let [classes (cond-> (icon-classes {:size size})
                         class (str " " class))
               svg-attrs (merge {:xmlns "http://www.w3.org/2000/svg"
                                 :viewBox "0 0 24 24"
                                 :fill (if filled "currentColor" "none")
                                 :stroke (if filled "none" "currentColor")
                                 :stroke-width "2"
                                 :stroke-linecap "round"
                                 :stroke-linejoin "round"
                                 :class classes
                                 :aria-hidden "true"}
                                attrs)]
           (into [:svg svg-attrs] paths))

         :cljs
         (let [cls (icon-class-list {:size size})
               classes (cond-> cls class (conj class))
               svg-attrs (merge {:xmlns "http://www.w3.org/2000/svg"
                                 :viewBox "0 0 24 24"
                                 :fill (if filled "currentColor" "none")
                                 :stroke (if filled "none" "currentColor")
                                 :stroke-width "2"
                                 :stroke-linecap "round"
                                 :stroke-linejoin "round"
                                 :class classes
                                 :aria-hidden "true"}
                                attrs)]
           (into [:svg svg-attrs] paths))

         :clj
         (let [classes (cond-> (icon-classes {:size size})
                         class (str " " class))
               svg-attrs (merge {:xmlns "http://www.w3.org/2000/svg"
                                 :viewBox "0 0 24 24"
                                 :fill (if filled "currentColor" "none")
                                 :stroke (if filled "none" "currentColor")
                                 :stroke-width "2"
                                 :stroke-linecap "round"
                                 :stroke-linejoin "round"
                                 :class classes
                                 :aria-hidden "true"}
                                attrs)]
           (into [:svg svg-attrs] paths))))))
