(ns clojuress.impl.rserve.clj-to-java
  (:import (org.rosuda.REngine REXP REXPString REXPSymbol REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named)))



(declare clj->java)

(defn ->rexp-string
  [xs]
  (REXPString.
   (into-array
    (map (fn [x]
           (cond (nil? x)            nil
                 (instance? Named x) (name x)
                 :else               (str x)))
         xs))))

(defn ->numeric-vector
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPDouble/NA
                (double x))))
       double-array
       (REXPDouble.)))

(defn ->integer-vector
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPInteger/NA
                (int x))))
       int-array
       (REXPInteger.)))




(defn primitive-type [obj]
  (cond (nil? obj)              :na
        (integer? obj)          :integer
        (number? obj)           :numeric
        (string? obj)           :character
        (keyword? obj)          :factor
        (instance? Boolean obj) :logical
        :else                   nil))

(def valid-interpretations {:na        [:integer :numeric :character :factor :logical]
                            :integer   [:integer :numeric :character]
                            :numeric   [:numeric :character]
                            :character [:character]
                            :factor    [:factor :character]
                            :logical   [:logical :character]})

(def interpretations-priorities
  (->> valid-interpretations
       (mapcat val)
       frequencies))

(defn finest-primitive-type [sequential]
  (let [n-elements (count sequential)]
    (->> sequential
         (mapcat (fn [elem]
                   (-> elem primitive-type valid-interpretations)))
         frequencies
         (filter (fn [[_ n]]
                   (= n n-elements)))
         (map key)
         (sort-by interpretations-priorities)
         first)))



(def primitive-vector-ctors
  {:integer   ->integer-vector
   :numeric   ->numeric-vector
   :character nil
   :factor    nil
   :logical   nil})

(defn ->primitive-vector [sequential]
  (when-let [primitive-type (finest-primitive-type sequential)]
    ((primitive-vector-ctors primitive-type) sequential)))



(defn clj->java
  [obj]
  (cond
    ;; nil
    (nil? obj)
    (REXPNull.)
    ;; basic types
    (primitive-type obj)
    (clj->java [obj])
    ;; a sequential structure
    (sequential? obj)
    (->primitive-vector obj)))
