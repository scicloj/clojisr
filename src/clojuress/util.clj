(ns clojuress.util
  (:require [com.rpl.specter :as specter]))

(defn first-if-one
  "Given a vector with a single element, first-if-one extracts that element; acts as identity otherwise.
 
       (first-if-one [:abc])
       => :abc
 
       (first-if-one [:abc :def])
       => [:abc :def]
 
       (first-if-one :abc)
       => :abc"
  {:added "0.1"}
  [form]
  (if (and (vector? form)
           (-> form count (= 1)))
    (first form)
    form))

(defn fmap
  "fmap updates the values of a given map
  using a given function
 
  (->> {:a 1
        :b 2}
       (fmap inc))
  => {:a 2
      :b 3}"
  {:added "0.1"} [f m]
  (specter/transform [specter/MAP-VALS]
                     f
                     m))


(defn starts-with? [s v]
  (and (sequential? s)
       (-> s first (= v))))
