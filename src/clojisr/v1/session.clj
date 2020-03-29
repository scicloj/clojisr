(ns clojisr.v1.session
  (:refer-clojure :exclude [time])
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.objects-memory :as mem]
            [clojisr.v1.gc :as gc]
            [clojisr.v1.util :refer [exception-cause get-free-port]]
            [clojure.tools.logging.readable :as log])
  (:import [java.io File]))


(defonce sessions (atom {}))

(defonce defaults (atom {}))
(let [port (get-free-port)]
  (log/info [::setting-default-port
             {:port port}])
  (swap! defaults assoc :port port))

(defn set-default-session-type! [session-type]
  (swap! defaults assoc :session-type session-type))

(defn set-default-session-type-if-missing! [session-type]
  (swap! defaults update :session-type
         (fn [current-session-type]
           (or current-session-type session-type))))

(defn apply-defaults [session-args]
  (merge @defaults session-args))

(defonce session-type->make-fn (atom {}))

(defn add-session-type!
  [session-type make-fn]
  (swap! session-type->make-fn
         assoc session-type make-fn))

(defn make [session-args]
  (let [id session-args
        {:keys [session-type] :as actual-session-args} (apply-defaults
                                                        session-args)
        make-fn (@session-type->make-fn session-type)]
    (log/info [::make-session
               {:action :new-session
                :id id
                :actual-session-args actual-session-args}])
    (make-fn
     id
     actual-session-args)))

(defonce last-clean-time (atom (System/currentTimeMillis)))

(defn clean-r-orphans []
  "Clean garbage collected RObjects on the R side."
  (when (> (- (System/currentTimeMillis) 120000) ;; every 2 minutes
           @last-clean-time)
    (let [curr (count (gc/ptr-set))]
      (gc/clear-reference-queue)
      (log/info [::gc-clean-r-objects {:objects-before curr
                                       :objects-after (count (gc/ptr-set))}]))
    (reset! last-clean-time (System/currentTimeMillis))))

(defn fetch [session-args]
  (clean-r-orphans)
  (@sessions session-args))

(defn discard [session-args]
  (gc/clear-reference-queue)
  (when-let [session (fetch session-args)]
    (log/info [::discard-session {:session-args session-args}])
    (prot/close session)
    (swap! sessions dissoc session-args)))

(defn discard-default []
  (discard nil))

(defn discard-all []
  (gc/clear-reference-queue)
  (doseq [[session-args session] @sessions]
    (log/info [::discard-all-sessions {:session-args session-args}])
    (prot/close session))
  (reset! sessions {}))

(defn init-memory [session]
  (prot/eval-r->java session mem/init-session-memory-code)
  session)

(defn init [session]
  (init-memory session)
  ;; TODO: Why is this necessary?
  (try
    (prot/eval-r->java session (format "setwd(\"%s\")" (.getAbsolutePath (File. "."))))
    (prot/eval-r->java session "print('.')")
    (catch Exception e (log/error [::init {:message "Session initialization failed."
                                           :exception (exception-cause e)}])))
  session)

(defn make-and-init [session-args]
  (let [session (make session-args)]
    (swap! sessions assoc session-args session)
    (init session)))

(defn fetch-or-make [session-args]
  (or (fetch session-args)
      (make-and-init session-args)))

(defn fresh? [session]
  (-> session
      prot/id
      fetch
      (= session)))

(defn fetch-or-make-and-init [session-args]
  (-> session-args
      fetch-or-make
      init))

