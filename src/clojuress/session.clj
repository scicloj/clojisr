(ns clojuress.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.protocols]
            [clojuress.impl.rserve.session]))

(def sessions (atom {}))

(def defaults
  (atom
   {:session-type :rserve}))

(defn make [session-args]
  (let [{:keys [session-type]} (merge @defaults
                                      session-args)]
    (case session-type
      :rserve (clojuress.impl.rserve.session/make session-args))))

(defn get [session-args]
  (or (@sessions session-args)
      (let [asession (make session-args)]
        (swap! sessions assoc session-args asession)
        asession)))
