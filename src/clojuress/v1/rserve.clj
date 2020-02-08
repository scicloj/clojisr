(ns clojuress.v1.rserve
  (:require [clojuress.v1.session :as session]
            [clojuress.v1.impl.rserve.session :refer [make]]))

(session/add-session-type!
 :rserve make)

(defn set-as-default! []
  (session/set-default-session-type! :rserve))

(defn set-as-default-if-missing! []
  (session/set-default-session-type-if-missing! :rserve))

(set-as-default-if-missing!)
