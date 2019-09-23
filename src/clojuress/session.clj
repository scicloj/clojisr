(ns clojuress.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.protocols :as prot]
            [clojuress.impl.rserve.session]
            [clojuress.rlang :as rlang]))

(def sessions (atom {}))

(def defaults
  (atom
   {:session-type :rserve}))


(defn make [session-args]
  (let [{:keys [session-type]} (merge @defaults
                                      session-args)]
    (case session-type
      :rserve (clojuress.impl.rserve.session/make
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

(defn make-and-init [session-args]
  (let [session (make session-args)]
    (swap! sessions assoc session-args session)
    (rlang/init-session session)
    session))

(defn fetch-or-make [session-args]
  (or (fetch session-args)
      (make-and-init session-args)))

(defn fresh? [session]
  (-> session
      prot/session-args
      fetch
      (= session)))
