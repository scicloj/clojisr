(ns clojuress.session
  (:refer-clojure :exclude [time])
  (:require [clojuress.protocols]
            [clojuress.rserve.session]))

(def sessions (atom {}))

(defn make [{:keys [session-type]
                     :or   {session-type :rserve}
                     :as   session-args}]
  (case session-type
    :rserve (clojuress.rserve.session/make session-args)))

(defn get [session-args]
  (or (@sessions session-args)
      (let [asession (make session-args)]
        (swap! sessions assoc session-args asession)
        asession)))
