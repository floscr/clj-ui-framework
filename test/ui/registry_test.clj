(ns ui.registry-test
  "Component metadata sidecars (src/ui/<name>.edn) must be valid and the
   generated markdown must render for every documented component."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [tools.registry :as registry]))

(deftest sidecars-valid-test
  (let [{:keys [invalid missing]} (registry/validate-all)]
    (testing "every sidecar passes schema + prop checks"
      (is (empty? invalid) (pr-str invalid)))
    (testing "every component has a sidecar (src/ui/<name>.edn)"
      (is (empty? missing) (str "missing sidecars: " (str/join ", " (map name missing)))))))

(deftest component-md-test
  (let [comps (registry/components)
        by-id (into {} (map (juxt :id identity) comps))]
    (doseq [c comps :when (:meta c)]
      (let [md (registry/component-md c by-id)]
        (testing (str "markdown for " (:id c))
          (is (str/starts-with? md (str "# " (get-in c [:meta :title]))))
          (is (str/includes? md "## When to use"))
          (is (str/includes? md "## API reference"))
          (is (str/includes? md "## Guidance for AI tools")))))))

(deftest llms-txt-test
  (let [txt (registry/llms-txt (registry/components))]
    (is (str/starts-with? txt "# clj-ui"))
    (is (str/includes? txt "## Rules for agents"))
    (is (str/includes? txt "## Components"))))
