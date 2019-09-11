(ns clojuress.impl.rserve.session
  (:require [clojuress.protocols :as prot]
            [clojuress.impl.rserve.proc :as proc]
            [clojuress.impl.rserve.java :as java]
            [clojuress.impl.rserve.java-to-clj :as java-to-clj]
            [clojuress.impl.rserve.clj-to-java :as clj-to-java]
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
  (java->r-set [session varname java-obj]
    ;; Unlike (.assign r-connection ...), the following approach
    ;; allows for a varname like "abc$xyz".
    (.eval
     r-connection
     (java/assignment varname java-obj)
     nil
     true))
  (get-r->java [session varname]
    (.parseAndEval r-connection varname))
  (java->specified-type [session java-obj typ]
    (java-to-clj/java->specified-type java-obj typ))
  (java->naive-clj [session java-obj]
    (java-to-clj/java->naive-clj java-obj))
  (java->clj [session java-obj]
    (java-to-clj/java->clj java-obj))
  (clj->java [session clj-obj]
    (clj-to-java/clj->java clj-obj)))

(def stop-loops? (atom false))

(defn rserve-print-loop [rserve]
  (async/go-loop []
    (doseq [^BufferedReader reader
            (-> rserve
                ((juxt :out :err)))]
      (loop []
        (when (.ready reader)
          (let [line (.readLine reader)]
            (when-not
                (re-find
                 ;; Just avoidingg this confusing message.
                 #"(This session will block until Rserve is shut down)" line)
              (println line)))
          (recur))))
    (Thread/sleep 100)
    (if (not @stop-loops?)
      (recur))) )

(defn make [session-args]
  (let [{:keys
         [host
          port
          spawn-rserve?]} (merge @defaults
                                 session-args)
        rserve            (when spawn-rserve?
                            (assert (= host "localhost"))
                            (proc/start-rserve {:port  port
                                                :sleep 500}))]
    (rserve-print-loop rserve)
    (->RserveSession session-args
                     (RConnection. host port)
                     rserve)))

(comment

  (reset! stop-loops? true)

  (reset! stop-loops? false)

  )
