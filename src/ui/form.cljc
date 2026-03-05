(ns ui.form
  (:require [clojure.string :as str]
            [ui.icon :as icon]
            [ui.tooltip :as tooltip]))

;; In squint, keywords are strings — name is identity
#?(:squint (defn- kw-name [s] s)
   :cljs   (defn- kw-name [s] (name s))
   :clj    (defn- kw-name [s] (name s)))

;; ── Form field wrapper ──────────────────────────────────────────────

(defn form-field-class-list
  "Returns a vector of CSS class strings for a form field wrapper."
  [{:keys [error]}]
  (cond-> ["form-field"]
    error (conj "form-field--error")))

(defn form-field-classes
  "Returns a space-joined class string for a form field wrapper."
  [opts]
  (str/join " " (form-field-class-list opts)))

(defn- error-icon
  "Render a circle-x icon wrapped in a tooltip showing the error text."
  [error]
  (tooltip/tooltip {:text error :class "form-error-icon"}
    (icon/icon {:icon-name :circle-x :size :sm})))

(defn form-field
  "Render a form field wrapper with label, hint, and error support.

   Props:
     :label - label text
     :hint  - hint text shown below input
     :error - error text shown below input (also sets error state)
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [label hint error class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> (form-field-classes {:error error})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (cond-> (if label
                       [[:label {:class "form-label"} label]]
                       [])
               true (into (if error
                            [(into [:div {:class "form-field-control"}]
                                   (conj (vec children) (error-icon error)))]
                            children))
               hint (conj [:small {:class "form-hint"} hint]))))

     :cljs
     (let [cls (form-field-class-list {:error error})
           classes (cond-> cls class (conj class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (cond-> (if label
                       [[:label {:class ["form-label"]} label]]
                       [])
               true (into (if error
                            [(into [:div {:class ["form-field-control"]}]
                                   (conj (vec children) (error-icon error)))]
                            children))
               hint (conj [:small {:class ["form-hint"]} hint]))))

     :clj
     (let [classes (cond-> (form-field-classes {:error error})
                     class (str " " class))
           base-attrs (merge {:class classes} attrs)]
       (into [:div base-attrs]
             (cond-> (if label
                       [[:label {:class "form-label"} label]]
                       [])
               true (into (if error
                            [(into [:div {:class "form-field-control"}]
                                   (conj (vec children) (error-icon error)))]
                            children))
               hint (conj [:small {:class "form-hint"} hint]))))))

;; ── Text input ──────────────────────────────────────────────────────

(defn form-input-class-list
  "Returns a vector of CSS class strings for a form input."
  [{:keys [error]}]
  (cond-> ["form-input"]
    error (conj "form-input--error")))

(defn form-input-classes
  "Returns a space-joined class string for a form input."
  [opts]
  (str/join " " (form-input-class-list opts)))

(defn form-input
  "Render a text input element.

   Props:
     :type        - :text, :email, :password, :date, :datetime-local, etc.
     :placeholder - placeholder text
     :value       - input value
     :disabled    - boolean
     :error       - boolean, adds error styling
     :on-change   - change handler (ignored in :clj target)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes"
  [{:keys [type placeholder value disabled error on-change class attrs] :as _props}]
  (let [input-type (or (some-> type kw-name) "text")]
    #?(:squint
       (let [classes (cond-> (form-input-classes {:error error})
                       class (str " " class))]
         [:input (cond-> (merge {:class classes :type input-type} attrs)
                   placeholder (assoc :placeholder placeholder)
                   value       (assoc :value value)
                   disabled    (assoc :disabled true)
                   on-change   (assoc :on-change on-change))])

       :cljs
       (let [cls (form-input-class-list {:error error})
             classes (cond-> cls class (conj class))]
         [:input (cond-> (merge {:class classes :type input-type} attrs)
                   placeholder (assoc :placeholder placeholder)
                   value       (assoc :value value)
                   disabled    (assoc :disabled true)
                   on-change   (assoc-in [:on :change] on-change))])

       :clj
       (let [classes (cond-> (form-input-classes {:error error})
                       class (str " " class))]
         [:input (cond-> (merge {:class classes :type input-type} attrs)
                   placeholder (assoc :placeholder placeholder)
                   value       (assoc :value value)
                   disabled    (assoc :disabled true))]))))

;; ── Textarea ────────────────────────────────────────────────────────

(defn form-textarea-class-list
  "Returns a vector of CSS class strings for a form textarea."
  [{:keys [error]}]
  (cond-> ["form-textarea"]
    error (conj "form-textarea--error")))

(defn form-textarea-classes
  "Returns a space-joined class string for a form textarea."
  [opts]
  (str/join " " (form-textarea-class-list opts)))

(defn form-textarea
  "Render a textarea element.

   Props:
     :placeholder - placeholder text
     :value       - textarea value
     :disabled    - boolean
     :error       - boolean, adds error styling
     :on-change   - change handler (ignored in :clj target)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes"
  [{:keys [placeholder value disabled error on-change class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> (form-textarea-classes {:error error})
                     class (str " " class))]
       [:textarea (cond-> (merge {:class classes} attrs)
                    placeholder (assoc :placeholder placeholder)
                    disabled    (assoc :disabled true)
                    on-change   (assoc :on-change on-change))
        (or value "")])

     :cljs
     (let [cls (form-textarea-class-list {:error error})
           classes (cond-> cls class (conj class))]
       [:textarea (cond-> (merge {:class classes} attrs)
                    placeholder (assoc :placeholder placeholder)
                    disabled    (assoc :disabled true)
                    on-change   (assoc-in [:on :change] on-change))
        (or value "")])

     :clj
     (let [classes (cond-> (form-textarea-classes {:error error})
                     class (str " " class))]
       [:textarea (cond-> (merge {:class classes} attrs)
                    placeholder (assoc :placeholder placeholder)
                    disabled    (assoc :disabled true))
        (or value "")])))

;; ── Select ──────────────────────────────────────────────────────────

(defn form-select
  "Render a select dropdown element.

   Props:
     :options     - vector of {:value \"v\" :label \"Label\"} or strings
     :placeholder - placeholder option text
     :value       - currently selected value
     :disabled    - boolean
     :on-change   - change handler (ignored in :clj target)
     :class       - additional CSS classes
     :attrs       - additional HTML attributes"
  [{:keys [options placeholder value disabled on-change class attrs] :as _props}]
  (let [opts (mapv (fn [o]
                     (if (string? o)
                       {:value o :label o}
                       o))
                   options)]
    #?(:squint
       (let [classes (cond-> "form-select"
                       class (str " " class))]
         (into [:select (cond-> (merge {:class classes} attrs)
                          disabled  (assoc :disabled true)
                          on-change (assoc :on-change on-change))]
               (cond-> (mapv (fn [o]
                               [:option {:value (:value o)} (:label o)])
                             opts)
                 placeholder (into [[:option {:value "" :disabled true :selected (nil? value)} placeholder]]))))

       :cljs
       (let [cls (cond-> ["form-select"] class (conj class))]
         (into [:select (cond-> (merge {:class cls} attrs)
                          disabled  (assoc :disabled true)
                          on-change (assoc-in [:on :change] on-change))]
               (cond-> (mapv (fn [o]
                               [:option {:value (:value o)} (:label o)])
                             opts)
                 placeholder (into [[:option {:value "" :disabled true :selected (nil? value)} placeholder]]))))

       :clj
       (let [classes (cond-> "form-select"
                       class (str " " class))]
         (into [:select (cond-> (merge {:class classes} attrs)
                          disabled (assoc :disabled true))]
               (cond-> (mapv (fn [o]
                               [:option {:value (:value o)} (:label o)])
                             opts)
                 placeholder (into [[:option {:value "" :disabled true :selected (nil? value)} placeholder]])))))))

;; ── Checkbox ────────────────────────────────────────────────────────

(defn form-checkbox
  "Render a checkbox with label.

   Props:
     :label     - label text
     :checked   - boolean
     :disabled  - boolean
     :on-change - change handler (ignored in :clj target)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [label checked disabled on-change class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> "form-field form-field--inline"
                     class (str " " class))]
       [:label (merge {:class classes} attrs)
        [:input (cond-> {:class "form-checkbox" :type "checkbox"}
                  checked   (assoc :checked true)
                  disabled  (assoc :disabled true)
                  on-change (assoc :on-change on-change))]
        (when label [:span label])])

     :cljs
     (let [cls (cond-> ["form-field" "form-field--inline"] class (conj class))]
       [:label (merge {:class cls} attrs)
        [:input (cond-> {:class ["form-checkbox"] :type "checkbox"}
                  checked   (assoc :checked true)
                  disabled  (assoc :disabled true)
                  on-change (assoc-in [:on :change] on-change))]
        (when label [:span label])])

     :clj
     (let [classes (cond-> "form-field form-field--inline"
                     class (str " " class))]
       [:label (merge {:class classes} attrs)
        [:input (cond-> {:class "form-checkbox" :type "checkbox"}
                  checked  (assoc :checked true)
                  disabled (assoc :disabled true))]
        (when label [:span label])])))

;; ── Radio group ─────────────────────────────────────────────────────

(defn form-radio-group
  "Render a radio button group inside a fieldset.

   Props:
     :label     - legend text
     :name      - radio group name
     :options   - vector of {:value \"v\" :label \"Label\"} or strings
     :value     - currently selected value
     :disabled  - boolean
     :on-change - change handler (ignored in :clj target)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [label radio-name options radio-value disabled on-change class attrs] :as _props}]
  (let [opts (mapv (fn [o]
                     (if (string? o)
                       {:value o :label o}
                       o))
                   options)]
    #?(:squint
       (let [classes (cond-> "form-fieldset form-fieldset--inline"
                       class (str " " class))]
         (into [:fieldset (merge {:class classes} attrs)]
               (cond-> []
                 label (conj [:legend {:class "form-legend"} label])
                 true  (into (mapv (fn [o]
                                     [:label
                                      [:input (cond-> {:class "form-radio" :type "radio"
                                                       :name (or radio-name "radio")}
                                                (some? (:value o))  (assoc :value (:value o))
                                                (= radio-value (:value o)) (assoc :checked true)
                                                disabled            (assoc :disabled true)
                                                on-change           (assoc :on-change on-change))]
                                      (:label o)])
                                   opts)))))

       :cljs
       (let [cls (cond-> ["form-fieldset" "form-fieldset--inline"] class (conj class))]
         (into [:fieldset (merge {:class cls} attrs)]
               (cond-> []
                 label (conj [:legend {:class ["form-legend"]} label])
                 true  (into (mapv (fn [o]
                                     [:label
                                      [:input (cond-> {:class ["form-radio"] :type "radio"
                                                       :name (or radio-name "radio")}
                                                (some? (:value o))  (assoc :value (:value o))
                                                (= radio-value (:value o)) (assoc :checked true)
                                                disabled            (assoc :disabled true)
                                                on-change           (assoc-in [:on :change] on-change))]
                                      (:label o)])
                                   opts)))))

       :clj
       (let [classes (cond-> "form-fieldset form-fieldset--inline"
                       class (str " " class))]
         (into [:fieldset (merge {:class classes} attrs)]
               (cond-> []
                 label (conj [:legend {:class "form-legend"} label])
                 true  (into (mapv (fn [o]
                                     [:label
                                      [:input (cond-> {:class "form-radio" :type "radio"
                                                       :name (or radio-name "radio")}
                                                (some? (:value o))  (assoc :value (:value o))
                                                (= radio-value (:value o)) (assoc :checked true)
                                                disabled            (assoc :disabled true))]
                                      (:label o)])
                                   opts))))))))

;; ── File input ──────────────────────────────────────────────────────

(defn form-file
  "Render a file input element.

   Props:
     :accept    - accepted file types (e.g. \"image/*,.pdf\")
     :multiple  - boolean, allow multiple files
     :disabled  - boolean
     :on-change - change handler (ignored in :clj target)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [accept multiple disabled on-change class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> "form-file"
                     class (str " " class))]
       [:input (cond-> (merge {:class classes :type "file"} attrs)
                 accept    (assoc :accept accept)
                 multiple  (assoc :multiple true)
                 disabled  (assoc :disabled true)
                 on-change (assoc :on-change on-change))])

     :cljs
     (let [cls (cond-> ["form-file"] class (conj class))]
       [:input (cond-> (merge {:class cls :type "file"} attrs)
                 accept    (assoc :accept accept)
                 multiple  (assoc :multiple true)
                 disabled  (assoc :disabled true)
                 on-change (assoc-in [:on :change] on-change))])

     :clj
     (let [classes (cond-> "form-file"
                     class (str " " class))]
       [:input (cond-> (merge {:class classes :type "file"} attrs)
                 accept   (assoc :accept accept)
                 multiple (assoc :multiple true)
                 disabled (assoc :disabled true))])))

;; ── Range input ─────────────────────────────────────────────────────

(defn form-range
  "Render a range slider input.

   Props:
     :min       - minimum value
     :max       - maximum value
     :step      - step increment
     :value     - current value
     :disabled  - boolean
     :on-change - change handler (ignored in :clj target)
     :class     - additional CSS classes
     :attrs     - additional HTML attributes"
  [{:keys [min max step value disabled on-change class attrs] :as _props}]
  #?(:squint
     (let [classes (cond-> "form-range"
                     class (str " " class))]
       [:input (cond-> (merge {:class classes :type "range"} attrs)
                 (some? min) (assoc :min min)
                 (some? max) (assoc :max max)
                 (some? step) (assoc :step step)
                 (some? value) (assoc :value value)
                 disabled     (assoc :disabled true)
                 on-change    (assoc :on-change on-change))])

     :cljs
     (let [cls (cond-> ["form-range"] class (conj class))]
       [:input (cond-> (merge {:class cls :type "range"} attrs)
                 (some? min) (assoc :min min)
                 (some? max) (assoc :max max)
                 (some? step) (assoc :step step)
                 (some? value) (assoc :value value)
                 disabled     (assoc :disabled true)
                 on-change    (assoc-in [:on :change] on-change))])

     :clj
     (let [classes (cond-> "form-range"
                     class (str " " class))]
       [:input (cond-> (merge {:class classes :type "range"} attrs)
                 (some? min) (assoc :min min)
                 (some? max) (assoc :max max)
                 (some? step) (assoc :step step)
                 (some? value) (assoc :value value)
                 disabled     (assoc :disabled true))])))

;; ── Input group ─────────────────────────────────────────────────────

(defn form-group
  "Render an input group (combined inputs, addons, and buttons).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "form-group"
                     class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))

     :cljs
     (let [cls (cond-> ["form-group"] class (conj class))]
       (into [:div (merge {:class cls} attrs)] children))

     :clj
     (let [classes (cond-> "form-group"
                     class (str " " class))]
       (into [:div (merge {:class classes} attrs)] children))))

(defn form-group-addon
  "Render an input group addon (static text before/after an input).

   Props:
     :class - additional CSS classes
     :attrs - additional HTML attributes"
  [{:keys [class attrs] :as _props} & children]
  #?(:squint
     (let [classes (cond-> "form-group-addon"
                     class (str " " class))]
       (into [:span (merge {:class classes} attrs)] children))

     :cljs
     (let [cls (cond-> ["form-group-addon"] class (conj class))]
       (into [:span (merge {:class cls} attrs)] children))

     :clj
     (let [classes (cond-> "form-group-addon"
                     class (str " " class))]
       (into [:span (merge {:class classes} attrs)] children))))
