(ns clojuress.v1.impl.rserve.session
  (:require [clojuress.v1.protocols :as prot]
            [clojuress.v1.impl.rserve.proc :as proc]
            [clojuress.v1.impl.rserve.java :as java]
            [clojuress.v1.impl.rserve.java-to-clj :as java-to-clj]
            [clojuress.v1.impl.rserve.clj-to-java :as clj-to-java]
            [clojure.core.async :as async]
            [cambium.core :as log])
  (:import (org.rosuda.REngine REXP REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           clojuress.v1.protocols.Session
           java.lang.Process
           java.io.BufferedReader))

(def defaults
  (atom
   {:port 6311
    :host "localhost"
    :spawn-rserve? true}))

(defrecord RserveSession [session-args
                          ^RConnection r-connection
                          rserve
                          closed]
  Session
  (close [session]
    (when r-connection
      (.close r-connection))
    (when rserve
      (proc/close rserve))
    (reset! closed true))
  (closed? [session]
    @closed)
  (session-args [session]
    session-args)
  (desc [session]
    session-args)
  (eval-r->java [session code]
    (log/debug [::eval {:code         code
                        :session-args (:session-args session)}])
    (java/try-eval-catching-errors code r-connection))
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
                     rserve
                     (atom false))))



(comment

  (reset! stop-loops? true)

  (reset! stop-loops? false)

  )
