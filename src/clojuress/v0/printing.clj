(ns clojuress.v0.printing
  (:import clojuress.v0.robject.RObject)
  (:require [clojuress.v0.using-sessions :as using-sessions]
            [clojure.pprint :as pp]
            [clojuress.v0.inspection :as inspection]))

(defn r-object->string-to-print [obj]
  (let [java-object (using-sessions/r->java obj)]
    (with-out-str
      (pp/pprint [['R
                   :object-name (:object-name obj)
                   :session-args (-> obj :session :session-args)
                   :r-class (inspection/r-class obj)]
                  ['->Java java-object]]))))

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
