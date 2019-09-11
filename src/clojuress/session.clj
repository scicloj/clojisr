(ns clojuress.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.protocols :as prot]
            [clojuress.impl.rserve.session]
            [clojuress.rlang :as rlang]))

(def sessions (atom {}))

(def defaults
  (atom
   {:session-type :rserve}))

(defn clean-all []
  (doseq [[session-args session] @sessions]
    (println [:closing session-args])
    (prot/close session))
  (reset! sessions {}))

(defn make [session-args]
  (let [{:keys [session-type]} (merge @defaults
                                      session-args)
        session (case session-type
                  :rserve (clojuress.impl.rserve.session/make
                           session-args))]
    (rlang/init-session session)
    session))

(defn fetch [session-args]
  (or (@sessions session-args)
      (let [asession (make session-args)]
        (swap! sessions assoc session-args asession)
        asession)))
