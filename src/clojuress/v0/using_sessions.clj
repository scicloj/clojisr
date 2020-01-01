(ns clojuress.v0.using-sessions
  (:require [tech.resource :as resource]
            [clojuress.v0.protocols :as prot]
            [clojuress.v0.objects-memory :as mem]
            [clojuress.v0.util :as util]
            [clojuress.v0.robject :refer [->RObject]]
            [cambium.core :as log])
  (:import clojuress.v0.robject.RObject))

(defn eval-code [code session]
  (let [obj-name (util/rand-name)
        returned (->> code
                      (mem/code-that-remembers obj-name)
                      (prot/eval-r->java session))]
    (assert (->> returned
                 (prot/java->clj session)
                 (= ["ok"])))
    (-> (->RObject obj-name session code)
        (resource/track
         #(do (log/info [::releasing obj-name])
              (mem/forget obj-name session))
         :gc))))

(defn java->r-specified-type [java-object type session]
  (prot/java->specified-type session java-object type))

(defn r-function-on-obj [{:keys [session] :as r-object}
                         function-name return-type]
  (->> r-object
       :object-name
       mem/object-name->memory-place
       (format "%s(%s)" function-name)
       (prot/eval-r->java session)
       (#(prot/java->specified-type session % return-type))))

(defn r->java [{:keys [session] :as r-object}]
  (->> r-object
       :object-name
       mem/object-name->memory-place
       (prot/get-r->java session)))

(defn java->r [java-object session]
  (if (instance? RObject java-object)
    java-object
    (let [obj-name (util/rand-name)]
      (prot/java->r-set session
                        (mem/object-name->memory-place
                         obj-name)
                        java-object)
      (->RObject obj-name session nil))))

