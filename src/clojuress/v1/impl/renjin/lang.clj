(ns clojuress.v1.impl.renjin.lang
  (:require [clojuress.v1.impl.renjin.engine :as engine]
            [clojuress.v1.util :as util])
  (:import (org.renjin.sexp Symbol Closure Environment DynamicEnvironment Null SEXP)
           (org.renjin.eval Context)
           (org.renjin.parser RParser)))


(defn ->symbol
  [k]
  (Symbol/get (name k)))

(defn ->env
  [engine bindings-map]
  (let [child-env (->> ^Context (engine/runtime-context engine)
                       (.getEnvironment)
                       (Environment/createChildEnvironment))]
    (doseq [[k v] bindings-map]
      (.setVariable ^DynamicEnvironment child-env
                    ^Symbol (->symbol k)
                    ^SEXP v))
    child-env))

(defn find-variable
  [^Environment env nam]
  (.findVariable env (->symbol nam)))


(defn parse-source
  [^String r-code]
  (RParser/parseSource r-code))

(defn eval-expressions
  [engine r-codes env]
  (let [context (.beginEvalContext ^Context (engine/runtime-context engine)
                                   env)]
    (->> r-codes
         (map (fn [s]
                (.evaluate context
                           (parse-source (str s "\n")))))
         last)))


(defn NULL->nil
  [obj]
  (if (= Null/INSTANCE obj)
    nil
    obj))

;; Extracting attributes of Renjin objects
;; (similar to Clojure metadata)
(defn ->attr
  [^SEXP sexp attr-name]
  (-> sexp
       (.getAttribute (Symbol/get (name attr-name)))
       NULL->nil
       (->> (mapv #(if (string? %)
                     (keyword %)
                     %)))))

(defn ->names
  [^SEXP sexp]
  (some->> (->attr sexp "names")
           (mapv keyword)))


(defn ->class
  [^SEXP sexp]
  (some->> (->attr sexp "class")
           (mapv keyword)))


(defn ->r-set [engine varname ^SEXP obj]
  (let [temp-name (util/rand-name)]
    (eval-expressions engine
                      [(str varname "<<-" temp-name)]
                      (->env engine {(keyword temp-name) obj}))))

