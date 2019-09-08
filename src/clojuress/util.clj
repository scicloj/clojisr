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

;; Adpoted and adapted from clojure-contrib
;; https://github.com/clojure/clojure-contrib/blob/a6a92b9b3d2bfd9a56e1e5e9cfba706d1aeeaae5/modules/with-ns/src/main/clojure/clojure/contrib/with_ns.clj#L20
(defmacro with-ns
  "Evaluates body in another namespace.  ns is either a namespace
  object or a symbol.  This makes it possible to define functions in
  namespaces other than the current one."
  [ns & body]
  `(do
     (if (not (find-ns ~ns))
       (create-ns ~ns))
     (binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body))))

