(ns clojisr.v1.session
  (:refer-clojure :exclude [time])
  (:require [clojisr.v1.protocols :as prot]
            [clojisr.v1.objects-memory :as mem]
            [clojisr.v1.gc :as gc]
            [cambium.core  :as log])
  (:import [java.io File]))


(defonce sessions (atom {}))

(defonce defaults (atom {}))

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
    (log/info [::making-a-new-session
               {:id                  id
                :actual-session-args actual-session-args}])
    (make-fn
     id
     actual-session-args)))

(defonce last-clean-time (atom (System/currentTimeMillis)))

;; TODO: GC should be done on the `session` level. Not globally. Currently GC can be called long after session is closed
;; and memory disposal is missed. Also, refreshing should be done for every object not only function.

(defn clean-r-orphans []
  "Clean garbage collected RObjects on the R side."
  (when (> (- (System/currentTimeMillis) 120000) ;; every 2 minutes
           @last-clean-time)
    (let [curr (count (gc/ptr-set))]
      (gc/clear-reference-queue)
      (log/info [::gc-called {:objects-before curr
                              :objects-after (count (gc/ptr-set))}]))
    (reset! last-clean-time (System/currentTimeMillis))))

(defn fetch [session-args]
  (clean-r-orphans)
  (@sessions session-args))

(defn discard [session-args]
  (gc/clear-reference-queue)
  (when-let [session (fetch session-args)]
    (log/info [::discarding session-args])
    (prot/close session)
    (swap! sessions dissoc session-args)))

(defn discard-default []
  (discard nil))

(defn discard-all []
  (gc/clear-reference-queue)
  (doseq [[session-args session] @sessions]
    (log/info [::discarding session-args])
    (prot/close session))
  (reset! sessions {}))

(defn init-memory [session]
  (prot/eval-r->java session mem/init-session-memory-code)
  session)

(defn init [session]
  (init-memory session)
  ;; TODO: Why is this necessary?
  (try
    (prot/eval-r->java session "print('.')")
    (prot/eval-r->java session (format "setwd(\"%s\")" (.getAbsolutePath (File. "."))))
    (catch Exception e nil))
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

