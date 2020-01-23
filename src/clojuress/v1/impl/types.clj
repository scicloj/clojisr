(ns clojuress.v1.impl.types)

(defn primitive-r-type [clj-val]
  (cond (nil? clj-val)              :na
        (integer? clj-val)          :integer
        (number? clj-val)           :numeric
        (string? clj-val)           :character
        (keyword? clj-val)          :factor
        (inst? clj-val)             :time
        (instance? Boolean clj-val) :logical
        :else                       nil))

(def valid-coercions
  {:na        [:integer :numeric :character :factor :logical :time]
   :integer   [:integer :numeric :character]
   :numeric   [:numeric :character]
   :character [:character]
   :factor    [:factor :character]
   :logical   [:logical :character]
   :time      [:time]})

(def coercions-priorities
  (->> valid-coercions
       (mapcat val)
       frequencies))

(defn finest-primitive-r-type [clj-sequential]
  (let [n-elements (count clj-sequential)]
    (->> clj-sequential
         (mapcat (fn [elem]
                   (-> elem primitive-r-type valid-coercions)))
         frequencies
         (filter (fn [[_ n]]
                   (= n n-elements)))
         (map key)
         (sort-by coercions-priorities)
         first)))

