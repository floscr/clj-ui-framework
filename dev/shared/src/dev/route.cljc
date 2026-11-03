(ns dev.route
  "URL paths of the demo SPAs (dev.replicant, dev.squint), mirroring the
   hiccup site's routes so a path like /components/tooltip?theme=light opens
   the same place on every target. A route is {:page kw :anchor section-id};
   the components page shows /components/<id> scrolled to that section.
   In production the SPAs live under /replicant/ and /squint/ (see
   dev.hiccup/handler), so paths carry that mount prefix."
  (:require [clojure.string :as str]
            [ui.util :as util]))

(def ^:private pages #{"calendar" "icons" "sidebar" "files" "dial"})

(defn mount
  "The production mount prefix (\"/replicant\", \"/squint\") of `pathname`,
   or \"\" on a dev server."
  [pathname]
  (or (second (re-find #"^(/(?:replicant|squint))(?:/|$)" pathname)) ""))

(defn- page-kw [s]
  ;; squint keywords are strings
  #?(:squint s :cljs (keyword s) :clj (keyword s)))

(defn parse
  "Route for a pathname without the mount prefix."
  [path]
  (let [path (str/replace path #"/+$" "")]
    (if-let [id (second (re-matches #"/components/([a-z0-9-]+)" path))]
      {:page :components :anchor id}
      (let [p (subs path (min 1 (count path)))]
        {:page (if (contains? pages p) (page-kw p) :components)}))))

(defn path
  "The pathname (without mount prefix) of a route."
  [{:keys [page anchor]}]
  (cond
    anchor                (str "/components/" anchor)
    (= page :components)  "/"
    :else                 (str "/" (util/kw-name page))))
