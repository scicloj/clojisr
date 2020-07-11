(ns clojisr.v1.renjin
  {:deprecated true}
  (:require [clojisr.v1.session :as session]))

(defn set-as-default!
  ^{:deprecated "Please use `clojisr.v1.r/set-default-session-type!` function."}
  [] (session/set-default-session-type! :renjin))

(defn set-as-default-if-missing!
  ^{:deprecated true}
  [] (session/set-default-session-type-if-missing! :renjin))

(set-as-default-if-missing!)
