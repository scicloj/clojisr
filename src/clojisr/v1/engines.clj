(ns clojisr.v1.engines
  (:require [clojisr.v1.impl.rserve.session :as rserve]
            [clojisr.v1.impl.renjin.session :as renjin]))

(defonce engines (atom {:rserve {:make rserve/make
                                 :default true}
                        :renjin {:make renjin/make}}))
