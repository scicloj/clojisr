(ns clojuress.v1.printing
  (:import clojuress.v1.robject.RObject)
  (:require [clojuress.v1.protocols :as prot]
            [clojuress.v1.refresh :as refresh]
            [clojure.pprint :as pp]))

(defn r-object->string-to-print [obj]
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
