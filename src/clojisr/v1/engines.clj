(ns clojisr.v1.engines
  (:require [clojisr.v1.impl.rserve.session :as rserve]))

(defonce engines (atom {:rserve {:make rserve/make
                                 :default true}}))
