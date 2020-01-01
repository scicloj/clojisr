(ns clojuress.v0.impl.rserve.printing
  (:require [clojuress.v0.impl.rserve.java-to-clj :refer [java->clj]]
            [clojure.pprint :as pp])
  (:import (org.rosuda.REngine REXP)))

(defn java-object->string-to-print [obj]
  (let [clj-obj (java->clj obj)]
    (with-out-str
      (pp/pprint
       (into [(symbol (.toDebugString obj))]
             (when (not= clj-obj obj)
               [['->Clj clj-obj]]))))))

;; Overriding print
(defmethod print-method REXP [obj ^java.io.Writer w]
  (->> obj
       java-object->string-to-print
       (.write w)))

;; Overriding pprint
(defmethod pp/simple-dispatch REXP [obj]
  (->> obj
       java-object->string-to-print
       println))
