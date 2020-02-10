(ns clojisr.v1.rserve
  (:require [clojisr.v1.session :as session]
            [clojisr.v1.impl.rserve.session :refer [make]]))

(session/add-session-type!
 :rserve make)

(defn set-as-default! []
  (session/set-default-session-type! :rserve))

(defn set-as-default-if-missing! []
  (session/set-default-session-type-if-missing! :rserve))

(set-as-default-if-missing!)
