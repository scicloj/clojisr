(ns clojuress.v1.printing
  (:import clojuress.v1.robject.RObject)
  (:require [clojuress.v1.using-sessions :as using-sessions]
            [clojure.pprint :as pp]
            [clojuress.v1.inspection :as inspection]
            [clojure.string :as string]))

(defn r-object->string-to-print [obj]
  (->> (using-sessions/r-function-on-obj
        obj
        "(function (o) capture.output(print(o)))"
        :strings)
       (string/join "\n")))

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
