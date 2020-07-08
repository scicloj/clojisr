(ns clojisr.v1.impl.renjin.call
  (:require [clojisr.v1.impl.renjin.engine :as engine]
            [clojisr.v1.util :as util]
            [clojisr.v1.impl.renjin.sexp :as sexp])
  (:import (org.renjin.sexp Symbol Environment DynamicEnvironment SEXP)
           (org.renjin.eval Context)
           (org.renjin.parser RParser)))

(set! *warn-on-reflection* true)

(defn ->env
  [engine bindings-map]
  (let [child-env (->> ^Context (engine/runtime-context engine)
                       (.getEnvironment)
                       (Environment/createChildEnvironment))]
    (doseq [[k v] bindings-map]
      (.setVariable ^DynamicEnvironment child-env
                    ^Symbol (sexp/->sexp-symbol k)
                    ^SEXP v))
    child-env))

(defn parse-source
  [^String r-code]
  (RParser/parseSource r-code))

(defn eval-expressions
  [engine r-codes env]
  (let [context (.beginEvalContext ^Context (engine/runtime-context engine)
                                   env)]
    (->> r-codes
         (map #(->> (str % "\n")
                    (parse-source)
                    (.evaluate context)))
         last)))

(defn ->r-set [engine varname ^SEXP obj]
  (let [temp-name (util/rand-name)]
    (eval-expressions engine
                      [(str varname "<<-" temp-name)]
                      (->env engine {temp-name obj}))))

