(ns clojuress.impl.rserve.session
  (:require [clojuress.protocols :as prot]
            [clojuress.impl.rserve.proc :as proc]
            [clojuress.impl.rserve.java :as java]
            [clojure.string :as string]
            [clojure.core.async :as async])
  (:import (org.rosuda.REngine REXP REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           clojuress.protocols.Session
           java.lang.Process
           java.io.BufferedReader))

(def defaults
  (atom
   {:port 6311
    :host "localhost"
    :spawn-rserve? true}))

(defrecord RserveSession [session-args
                          ^RConnection r-connection
                          rserve]
  Session
  (close [session]
    (when rserve
      (proc/close rserve)))
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
  (java->specified-type [session java-object type]
    (case type
      :ints    (.asIntegers ^REXP java-object)
      :doubles (.asDoubles ^REXP java-object)
      :strings (.asStrings ^REXP java-object)))
  (java->clj [session java-object]
    (java/java->clj java-object))
  (clj->java [session clj-object]
    (java/clj->java clj-object)))

(def abc (atom true))

(defn make [session-args]
  (let [{:keys
         [host
          port
          spawn-rserve?]} (merge @defaults
                                 session-args)
        rserve            (when spawn-rserve?
                            (assert (= host "localhost"))
                            (proc/start-rserve {:port  port
                                                :sleep 500}))
        session           (->RserveSession session-args
                                           (RConnection. host port)
                                           rserve)]
    (async/go-loop []
      (Thread/sleep 100)
      (doseq [^BufferedReader reader
              (-> rserve
                  ((juxt :out :err)))]
        (loop []
          (when (.ready reader)
            (println (.readLine reader))
            (recur))))
      (if @abc (recur)))
    session))

(comment

  (reset! abc true)

  (reset! abc false)


  (def rserve1
    (proc/start-rserve {:port  2222
                        :sleep 500}))

  rserve1

  (some-> rserve1 :out (.ready))
  (some-> rserve1 :out (.readLine))

  (some-> rserve1 :out)

  )


