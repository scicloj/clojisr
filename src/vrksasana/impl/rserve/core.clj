(ns vrksasana.impl.rserve.core
  (:require [vrksasana.impl.rserve.ground :as rserve-ground]))

(defn setup
  ([]
   (rserve-ground/setup {}))
  ([options]
   (rserve-ground/setup options)))


