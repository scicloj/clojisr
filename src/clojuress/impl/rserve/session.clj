(ns clojuress.impl.rserve.session
  (:require [clojuress.protocols :as prot]
            [clojuress.impl.rserve.proc :as proc]
            [clojure.java.shell :refer [sh]]
            [clojuress.impl.rserve.java :as java]
            [clojure.string :as string])
  (:import (org.rosuda.REngine REXP REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           clojuress.protocols.Session))

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
  (eval-r->java [session code]
    (.parseAndEval r-connection code))
  (java->r-set [session varname java-object]
    ;; Unlike (.assign r-connection ...), the following approach
    ;; allows for a varname like "abc$xyz".
    (.eval
     r-connection
     (java/call "<-"
                [(if (re-find #"\$" varname)
                   (->> (string/split varname #"\$")
                        (map java/rexp-symbol)
                        (java/call "$"))
                   (java/rexp-symbol varname))
                 java-object])
     nil
     true))
  (get-r->java [session varname]
    (.parseAndEval r-connection varname))
  (java->r-specified-type [session java-object type]
    (case type
      :ints    (.asIntegers ^REXP java-object)
      :doubles (.asDoubles ^REXP java-object)
      :strings (.asStrings ^REXP java-object)))
  (java->clj [session java-object]
    (java/java->clj java-object))
  (clj->java [session clj-object]
    (java/clj->java clj-object)))


(defn make [session-args]
  (let [{:keys [host port]} (merge @defaults
 session-args)]
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
             (butlast)))) ; avoid trailing newline

(defn start-rserve
  "Boot up RServe on default port in another process.
   Returns a map with a java.lang.Process that can be 'destroy'ed"
  ([] (start-rserve 6311 ""))
  ([port init-r]
   (let [rstr-temp (format "library(Rserve); run.Rserve(args='--no-save --slave', port=%s);" port)
         rstr      (if (empty? init-r )
                     rstr-temp
                     (str init-r ";" rstr-temp ))]
     (prn rstr)
     (proc/spawn (r-path)
                 "--no-save" ; don't save workspace when quitting
                 "--slave"
                 "-e" ; evaluate (boot server)
                 rstr))))


