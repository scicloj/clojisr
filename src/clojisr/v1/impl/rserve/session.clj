(ns clojisr.v1.impl.rserve.session
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.impl.rserve.proc :as proc]
            [clojisr.v1.impl.rserve.java :as java]
            [clojisr.v1.impl.rserve.java-to-clj :as java-to-clj]
            [clojisr.v1.impl.rserve.clj-to-java :as clj-to-java]
            [clojisr.v1.impl.rserve.packages :as packages]
            [clojisr.v1.impl.rserve.printing :as printing]
            [clojure.core.async :as async]
            [clojure.tools.logging.readable :as log])
  (:import (org.rosuda.REngine REXP REngineException REXPMismatchException)
           (org.rosuda.REngine.Rserve RConnection)
           clojisr.v1.protocols.Session
           java.lang.Process
           java.io.BufferedReader))

(def defaults
  (atom
   {:port 6311
    :host "localhost"
    :spawn-rserve? true}))

(defn close! [{:keys [^RConnection r-connection rserve]}]
  (when r-connection
    (.close r-connection))
  (when rserve
    (proc/close rserve))
  nil)

;; Session is valid when process is alive and when there is connection
;; if something is not true, ensure cleaning the rest and close the session
(defn active?-or-close! [{:keys [^RConnection r-connection rserve]
                          :as sess}]
  (let [state (and r-connection
                   rserve
                   (.isConnected r-connection)
                   (proc/alive? rserve))]
    (or state (close! sess))))

(defrecord RserveSession [id
                          session-args
                          ^RConnection r-connection
                          rserve]
  Session
  (close [session]
    (close! session))
  (closed? [session]
    (not (active?-or-close! session)))
  (id [session]
    id)
  (session-args [session]
    session-args)
  (desc [session]
    session-args)
  (eval-r->java [session code]
    (log/debug [::eval-r->java {:code         code
                                :session-args (:session-args session)}])
    (java/try-eval-catching-errors code r-connection))
  (java->r-set [session varname java-obj]
    ;; Unlike (.assign r-connection ...), the following approach
    ;; allows for a varname like "abc$xyz".
    (locking r-connection
      (.eval
       r-connection
       (java/assignment varname java-obj)
       nil
       true)))
  (java->specified-type [session java-obj typ]
    (java-to-clj/java->specified-type java-obj typ))
  (java->naive-clj [session java-obj]
    (java-to-clj/java->naive-clj java-obj))
  (java->clj [session java-obj]
    (java-to-clj/java->clj java-obj))
  (clj->java [session clj-obj]
    (clj-to-java/clj->java clj-obj))
  (print-to-string [session r-obj]
    (printing/print-to-string session r-obj))
  (package-symbol->r-symbol-names [session package-symbol]
    (packages/package-symbol->r-symbol-names
     session package-symbol)))

(defn rserve-print-loop [{:keys [rserve]
                          :as session}]
  (log/info [::rserve-print-loop {:action :started
                                  :session-args (:session-args session)}])
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
    (if (not (prot/closed? session))
      (recur)
      (log/info [::rserve-print-loop {:action :stopped
                                      :session-args (:session-args session)}]))))

(defn make [id session-args]
  (let [{:keys
         [host
          port
          spawn-rserve?]} (merge @defaults
                                 session-args)
        rserve            (when spawn-rserve?
                            ;; (assert (= host "localhost")) ;; why???
                            (proc/start-rserve {:port  port
                                                :sleep 500}))
        session (->RserveSession id
                                 session-args
                                 (RConnection. host port)
                                 rserve)]
    (rserve-print-loop session)
    session))

