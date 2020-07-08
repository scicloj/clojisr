(ns clojisr.v1.printing
  (:import clojisr.v1.robject.RObject)
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.refresh :as refresh]
            [clojure.pprint :as pp]))

(set! *warn-on-reflection* true)

(defn r-object->string-to-print
  ^String [obj]
  (if (refresh/fresh-object? obj)
    (prot/print-to-string (:session obj)
                          obj)
    "<an object of lost session>"))

;; Overriding print
(defmethod print-method RObject [obj ^java.io.Writer w]
  (->> obj
       r-object->string-to-print
       (.write w)))

;; Overriding pprint
(defmethod pp/simple-dispatch RObject [obj]
  (->> obj
       r-object->string-to-print
       println))
