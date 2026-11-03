(ns dev.route-test
  (:require [clojure.test :refer [deftest is testing]]
            [dev.route :as route]))

(deftest mount-test
  (is (= "" (route/mount "/components/tooltip")))
  (is (= "/replicant" (route/mount "/replicant/components/tooltip")))
  (is (= "/squint" (route/mount "/squint")))
  (is (= "" (route/mount "/squintish"))))

(deftest parse-test
  (testing "hiccup site paths open the matching SPA page"
    (is (= {:page :components :anchor "tooltip"} (route/parse "/components/tooltip")))
    (is (= {:page :components} (route/parse "/")))
    (is (= {:page :components} (route/parse "")))
    (is (= {:page :components} (route/parse "/all")))
    (is (= {:page :calendar} (route/parse "/calendar/"))))
  (testing "unknown paths fall back to the components page"
    (is (= {:page :components} (route/parse "/nope")))))

(deftest path-round-trip-test
  (doseq [p ["/" "/components/number-field" "/icons" "/dial"]]
    (is (= p (route/path (route/parse p))))))
