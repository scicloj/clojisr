(ns clojuress.rserve.wrap
  (:require [clojuress.protocols])
  (:import [org.rosuda.REngine REXPVector REXPDouble]
           [tech.v2.datatype ObjectReader ObjectWriter ObjectMutable
            ObjectIter MutableRemove]
           [java.lang UnsupportedOperationException]))

(defrecord RObject [o])



(defn as-list [o]
  )

(defn as-list [REXPVector v]
  (let [cached-convections {}])
  (reify
    ))


(def abc
  (REXPDouble. (double-array [1 2])))

(defn  [x]
  )

(extend-type REXPDouble
  clojure.lang.ISeqable
  (-seq [x]
    (-> x (.asDoubles) seq)))
