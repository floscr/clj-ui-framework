(ns dev.examples.table
  (:require [dev.example :refer [example]]
            [ui.table :as table]))

(def examples
  [(example :basic "Basic"
     (fn []
       (table/table {:headers ["Name" "Email" "Role" "Status"]
                     :rows [["Alice Johnson" "alice@example.com" "Admin" "Active"]
                            ["Bob Smith" "bob@example.com" "Editor" "Active"]
                            ["Carol White" "carol@example.com" "Viewer" "Pending"]]})))])
