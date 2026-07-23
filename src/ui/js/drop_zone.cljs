(ns drop-zone
  "Drag & drop runtime for [data-ui-drop-zone] elements (hiccup target).

   The server-rendered zone has no event handlers, so this runtime wires
   document-level delegated listeners that:

     - toggle .drop-zone-active while a drag hovers the zone
     - dispatch a `ui:drop-zone-files` CustomEvent (bubbles: true,
       detail: {files: File[]}) on the zone element when files are
       dropped onto it or selected via its hidden file input

   Consumers listen for the CustomEvent:

     document.addEventListener('ui:drop-zone-files', (e) => {
       e.detail.files // File[]
     });")

(defn- closest-zone [el]
  (when (and el (.-closest el))
    (.closest el "[data-ui-drop-zone]")))

(defn- emit-files! [zone files]
  (when (pos? (.-length files))
    (.dispatchEvent zone
                    (js/CustomEvent. "ui:drop-zone-files"
                                     #js {:bubbles true
                                          :detail #js {:files files}}))))

(defn- on-dragover [e]
  (when-let [zone (closest-zone (.-target e))]
    (.preventDefault e)
    (.add (.-classList zone) "drop-zone-active")))

(defn- on-dragleave [e]
  (when-let [zone (closest-zone (.-target e))]
    ;; Ignore dragleave fired when moving over child elements.
    (when-not (and (.-relatedTarget e)
                   (.contains zone (.-relatedTarget e)))
      (.remove (.-classList zone) "drop-zone-active"))))

(defn- on-drop [e]
  (when-let [zone (closest-zone (.-target e))]
    (.preventDefault e)
    (.remove (.-classList zone) "drop-zone-active")
    (emit-files! zone (js/Array.from (.. e -dataTransfer -files)))))

(defn- on-change [e]
  (let [input (.-target e)]
    (when (and input (.-matches input)
               (.matches input "input[type=\"file\"]"))
      (when-let [zone (closest-zone input)]
        (emit-files! zone (js/Array.from (.-files input)))))))

(defn init! []
  (.addEventListener js/document "dragover" on-dragover)
  (.addEventListener js/document "dragleave" on-dragleave)
  (.addEventListener js/document "drop" on-drop)
  (.addEventListener js/document "change" on-change))

(init!)
