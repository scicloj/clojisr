(ns clojisr.v1.rserve
  {:deprecated true}
  (:require [clojisr.v1.session :as session]))

(defn set-as-default!
  ^{:deprecated "Please use `clojisr.v1.r/set-default-session-type!` function."}
  [] (session/set-default-session-type! :rserve))

(defn set-as-default-if-missing!
  ^{:deprecated true}
  [] (session/set-default-session-type-if-missing! :rserve))

(set-as-default-if-missing!)
