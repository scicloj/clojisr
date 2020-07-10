(ns clojisr.v1.impl.rserve.session
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.impl.protocols :as iprot]
            [clojisr.v1.impl.rserve.rexp :as rexp] ;; ensure protocols implementation
            [clojisr.v1.impl.rserve.proc :as proc]
            [clojisr.v1.impl.rserve.call :as call]
            [clojisr.v1.impl.rserve.packages :as packages]
            [clojisr.v1.impl.rserve.printing :as printing]
            [clojure.core.async :as async]
            [clojure.tools.logging.readable :as log]
            [clojisr.v1.util :refer [exception-cause get-free-port]])
  (:import [org.rosuda.REngine.Rserve RConnection]
           [java.io BufferedReader]))

(set! *warn-on-reflection* true)

(def defaults
  (atom
   {:host "localhost"
    :spawn-rserve? true}))

(defn close! [{:keys [^RConnection r-connection rserve]}]
  (when r-connection
    (.close r-connection))
  (when rserve
    (proc/close rserve))
  nil)

;; Session is valid when there is connection, also when we have rserve, process should be active
;; if something is not true, ensure cleaning the rest and close the session
(defn active?-or-close! [{:keys [^RConnection r-connection rserve]
                          :as sess}]
  (let [state (and r-connection
                   (.isConnected r-connection)
                   (if-not rserve
                     true
                     (proc/alive? rserve)))]
    (or state (close! sess))))

(defrecord RserveSession [id
                          session-args
                          ^RConnection r-connection
                          rserve]
  prot/Session
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
    (log/debug [::eval-r->java {:code code
                                :session-args (:session-args session)}])
    (call/try-eval-catching-errors code r-connection))
  (java->r-set [session varname java-obj]
    ;; Unlike (.assign r-connection ...), the following approach
    ;; allows for a varname like "abc$xyz".
    (locking r-connection
      (.eval
       r-connection
       (call/assignment varname java-obj)
       nil
       true)))
  (print-to-string [session r-obj]
    (printing/print-to-string session r-obj))
  (package-symbol->r-symbol-names [session package-symbol]
    (packages/package-symbol->r-symbol-names
     session package-symbol))

  iprot/Engine
  (->nil [_] (rexp/->rexp-nil))
  (->symbol [_ x] (rexp/->rexp-symbol x))
  (->string-vector [_ xs] (rexp/->rexp-strings xs))
  (->numeric-vector [_ xs] (rexp/->rexp-doubles xs))
  (->integer-vector [_ xs] (rexp/->rexp-integers xs))
  (->logical-vector [_ xs] (rexp/->rexp-logical xs))
  (->factor [_ xs] (rexp/->rexp-factor xs))
  (->factor [_ ids levels] (rexp/->rexp-factor ids levels))
  (->list [_ vs] (rexp/->rexp-list vs))
  (->named-list [_ ks vs] (rexp/->rexp-named-list ks vs))
  (native? [_ x] (rexp/rexp? x)))

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
  (let [{:keys [host port spawn-rserve? init-r]} (merge @defaults
                                                        session-args)
        port (or port (get-free-port))
        rserve (when spawn-rserve?
                 (proc/start-rserve port init-r))]

    (when rserve ;; be sure the process is spawned
      (loop [attempts (int 5)]
        (Thread/sleep 500)
        (if (or (zero? attempts)
                (proc/alive? rserve))
          (when-not (proc/alive? rserve)
            (throw (Exception. "Can't create RServe process.")))
          (do
            (log/warn [::rserve-spawn {:message "Rserve is not alive yet, waiting 0.5s"}])
            (recur (dec attempts))))))

    (let [conn (loop [attempts (int 1)] ;; try 5 times to connect
                 (when (> attempts 5) ;; throw an Exception when can't connect
                   (throw (Exception. "Can't connect to RServe, please check host/port settings.")))
                 (Thread/sleep (* attempts 200))
                 (let [conn (try
                              (RConnection. host port)
                              (catch Exception e
                                (log/warn [::make-rserve {:exception (exception-cause e)
                                                          :message "Exception during connection to RServe, trying once more."}])))]
                   (if (and conn (.isConnected ^RConnection conn))
                     conn
                     (do
                       (log/warn [::make-rserve {:message "Waiting for connection to the RServe."}])
                       (recur (inc attempts))))))
          
          session (->RserveSession id
                                   session-args
                                   conn
                                   rserve)]      
      (when rserve (rserve-print-loop session))
      session)))
