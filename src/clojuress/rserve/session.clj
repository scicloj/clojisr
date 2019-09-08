(ns clojuress.rserve.session
  (:require [clojuress.protocols :as prot]
            [clojuress.rserve.proc :as proc]
            [clojure.java.shell :refer [sh]]
            [clojuress.rserve.jvm :as jvm]
            [clojure.string :as string])
  (:import (org.rosuda.REngine REXP REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           clojuress.protocols.Session))

(set! *warn-on-reflection* true)

(def defaults
  (atom
   {:port 6311
    :host "localhost"}))

(defrecord RserveSession [session-args
                          ^RConnection r-connection]
  Session
  (close [session])
  (desc [session]
    session-args)
  (eval->jvm [session code]
    (.parseAndEval r-connection code))
  (jvm->set [session varname jvm-object]
    ;; Unlike (.assign r-connection ...), the following approach
    ;; allows for a varname like "abc$xyz".
    (.eval
     r-connection
     (jvm/call "<-"
                [(if (re-find #"\$" varname)
                   (->> (string/split varname #"\$")
                        (map jvm/rexp-symbol)
                        (jvm/call "$"))
                   (jvm/rexp-symbol varname))
                 jvm-object])
     nil
     true))
  (get->jvm [session varname]
    (.parseAndEval r-connection varname))
  (jvm->specified-type [session jvm-object type]
    (case type
      :ints    (.asIntegers ^REXP jvm-object)
      :doubles (.asDoubles ^REXP jvm-object)
      :strings (.asStrings ^REXP jvm-object)))
  (->clj [session jvm-object]
    (jvm/->clj jvm-object))
  (clj-> [session clj-object]
    (jvm/clj-> clj-object)))


(defn make [session-args]
  (let [{:keys [host port]}
        (merge @defaults session-args)]
    (->RserveSession session-args
                     (RConnection. host port))))

;; Running Rserve -- copied from Rojure:
;; https://github.com/behrica/rojure

(defn- r-path
  "Find path to R executable"
  []
  {:post [(not= % "")]}
  (apply str
         (-> (sh "which" "R")
             (get :out)
             (butlast))))                                   ;; avoid trailing newline

(defn start-rserve
  "Boot up RServe on default port in another process.
   Returns a map with a java.lang.Process that can be 'destroy'ed"
  ([] (start-rserve 6311 ""))
  ([port init-r]
   (let [rstr-temp (format "library(Rserve); run.Rserve(args='--no-save --slave', port=%s);" port)
         rstr      (if (empty? init-r )
                     rstr-temp
                     (str init-r ";" rstr-temp )
                     )
         ]
     (prn rstr)
     (proc/spawn (r-path)
                 "--no-save"                                   ;; don't save workspace when quitting
                 "--slave"
                 "-e"                                          ;; evaluate (boot server)
                 rstr))))


