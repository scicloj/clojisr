(ns clojuress.v0.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.v0.protocols :as prot]
            [clojuress.v0.impl.rserve.session]
            [clojuress.v0.objects-memory :as mem]))

(def sessions (atom {}))

(def defaults
  (atom
   {:session-type :rserve}))


(defn make [session-args]
  (let [{:keys [session-type]} (merge @defaults
                                      session-args)]
    (case session-type
      :rserve (clojuress.v0.impl.rserve.session/make
               session-args))))


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
    (println [:discarding session-args])
    (prot/close session))
  (reset! sessions {}))

(defn init-memory [session]
  (prot/eval-r->java session mem/init-session-memory-code)
  session)

(defn init [session]
  (init-memory session))

(defn make-and-init [session-args]
  (let [session (make session-args)]
    (swap! sessions assoc session-args session)
    (init session)))

(defn fetch-or-make [session-args]
  (or (fetch session-args)
      (make-and-init session-args)))

(defn fresh? [session]
  (-> session
      prot/session-args
      fetch
      (= session)))

(defn fetch-or-make-and-init [session-args]
  (-> session-args
      fetch-or-make
      init))
