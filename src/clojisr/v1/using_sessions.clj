(ns clojisr.v1.using-sessions
  (:require [clojisr.v1.gc :as gc]
            [clojisr.v1.protocols :as prot]
            [clojisr.v1.objects-memory :as mem]
            [clojisr.v1.util :as util]
            [clojisr.v1.robject :refer [->RObject]]
            [clojisr.v1.known-classes :as known-classes]
            [clojure.tools.logging.readable :as log])
  (:import clojisr.v1.robject.RObject))

(defn random-object-name []
  (str mem/session-env "$" (util/rand-name)))

(defn ->robject [obj-name session code]
  (let [theclass (->> obj-name   
                      (format "class(%s)")
                      (prot/eval-r->java session)
                      (prot/java->clj session))]
    (->RObject obj-name session code theclass)))

(defn eval-code
  ([code session] (eval-code (random-object-name) code session))
  ([obj-name code session]
   (let [obj-name (or obj-name (random-object-name))
         returned (->> code
                       (mem/code-that-remembers obj-name)
                       (prot/eval-r->java session))]
     (assert (->> returned
                  (prot/java->clj session)
                  (= ["ok"])))
     (-> (->robject obj-name session code)
         (gc/track
          #(do (log/debug [::gc {:releasing obj-name}])
               (mem/forget obj-name session)))))))

(defn java->r-specified-type [java-object type session]
  (prot/java->specified-type session java-object type))

(defn r-function-on-obj [{:keys [session] :as r-object}
                         function-code return-type]
  (->> r-object
       :object-name
       (format "%s(%s)" function-code)
       (prot/eval-r->java session)
       (#(prot/java->specified-type session % return-type))))

(defn r->java [{:keys [session object-name] :as r-object}]
  (prot/eval-r->java session object-name))

(defn java->r [java-object session]
  (if (instance? RObject java-object)
    java-object
    (let [obj-name (random-object-name)]
      (prot/java->r-set session
                        obj-name
                        java-object)
      (->robject obj-name session nil))))

(defn function? [r-object]
  (and (instance? RObject r-object)
       (-> r-object
           :class
           known-classes/function-classes)))
