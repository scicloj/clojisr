(ns clojuress.v1.printing
  (:import clojuress.v1.robject.RObject)
  (:require [clojuress.v1.using-sessions :as using-sessions]
            [clojuress.v1.protocols :as prot]
            [clojure.pprint :as pp]
            [clojure.string :as string]))

(defn r-object->string-to-print [obj]
  (prot/print-to-string (:session obj)
                        obj))

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
