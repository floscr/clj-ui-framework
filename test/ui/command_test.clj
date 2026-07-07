(ns ui.command-test
  (:require [clojure.test :refer [deftest is testing]]
            [ui.command :as command]))

(deftest command-trigger-test
  (testing "renders a button wired to the target"
    (let [[tag attrs label] (command/command-trigger {:target "cmd"} "Search")]
      (is (= :button tag))
      (is (= "cmd" (:data-command-target attrs)))
      (is (= "dialog" (:aria-haspopup attrs)))
      (is (= "command-trigger" (:class attrs)))
      (is (string? (:onclick attrs)))
      (is (= "Search" label))))
  (testing "merges extra classes"
    (let [[_ attrs] (command/command-trigger {:target "cmd" :class "btn"} "x")]
      (is (= "command-trigger btn" (:class attrs))))))

(deftest command-dialog-test
  (testing "renders a dialog with id, hotkey and a search input"
    (let [[tag attrs search list*] (command/command-dialog
                                    {:id "cmd" :hotkey "mod+k"} "child")]
      (is (= :dialog tag))
      (is (= "cmd" (:id attrs)))
      (is (= "mod+k" (:data-command-hotkey attrs)))
      (is (= "dialog" (:role attrs)))
      (is (= "true" (:aria-modal attrs)))
      (is (= "command-search" (:class (second search))))
      (is (= "command-list" (:class (second list*))))))
  (testing "placeholder default and override"
    (let [[_ _ search] (command/command-dialog {:id "c"} )
          input (nth search 3)]
      (is (= "Type a command or search…" (:placeholder (second input)))))
    (let [[_ _ search] (command/command-dialog {:id "c" :placeholder "Go to…"})
          input (nth search 3)]
      (is (= "Go to…" (:placeholder (second input))))))
  (testing "empty state text lives in the list"
    (let [[_ _ _ list*] (command/command-dialog {:id "c" :empty "Nothing here"})
          empty-el (last list*)]
      (is (= "command-empty" (:class (second empty-el))))
      (is (= "Nothing here" (last empty-el))))))

(deftest command-group-test
  (testing "renders heading and items wrapper"
    (let [[tag attrs heading items*] (command/command-group {:heading "Suggestions"} "i")]
      (is (= :div tag))
      (is (= "command-group" (:class attrs)))
      (is (= "group" (:role attrs)))
      (is (= "command-group-heading" (:class (second heading))))
      (is (= "Suggestions" (last heading)))
      (is (= "command-group-items" (:class (second items*))))))
  (testing "no heading is omitted"
    (let [[_ _ items*] (command/command-group {} "i")]
      (is (= "command-group-items" (:class (second items*)))))))

(deftest command-item-test
  (testing "renders a button with a label span"
    (let [[tag attrs] (command/command-item {} "Calendar")]
      (is (= :button tag))
      (is (= "command-item" (:class attrs)))
      (is (= "option" (:role attrs)))
      (is (= "button" (:type attrs)))))
  (testing "url renders an anchor"
    (let [[tag attrs] (command/command-item {:url "/x"} "Go")]
      (is (= :a tag))
      (is (= "/x" (:href attrs)))
      (is (nil? (:type attrs)))))
  (testing "shortcut renders a kbd"
    (let [item (command/command-item {:shortcut "⌘P"} "Calendar")
          kbd  (last item)]
      (is (= :kbd (first kbd)))
      (is (= "command-shortcut" (:class (second kbd))))
      (is (= "⌘P" (last kbd)))))
  (testing "explicit value and disabled"
    (let [[_ attrs] (command/command-item {:value "cal" :disabled true} "Calendar")]
      (is (= "cal" (:data-command-value attrs)))
      (is (true? (:disabled attrs))))))
