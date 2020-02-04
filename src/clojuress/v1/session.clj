(ns clojuress.v1.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.v1.protocols :as prot]
            [clojuress.v1.impl.rserve.session]
            [clojuress.v1.objects-memory :as mem]
            [cambium.core  :as log]))

(def sessions (atom {}))

(def defaults
  (atom {:session-type :rserve}))

(defn apply-defaults [session-args]
  (merge @defaults session-args))

(defn make [session-args]
  (let [id session-args
        {:keys [session-type] :as merged-session-args}
        (apply-defaults session-args)]
    (case session-type
      :rserve (clojuress.v1.impl.rserve.session/make
               id
               merged-session-args))))

(defn fetch [session-args]
  (@sessions session-args))

(defn discard [session-args]
  (when-let [session (fetch session-args)]
    (prot/close session)
    (swap! sessions dissoc session-args)))

(defn discard-default []
  (discard nil))

(defn discard-all []
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

