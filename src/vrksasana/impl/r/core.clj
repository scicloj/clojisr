(ns vrksasana.impl.r.core
  (:require [vrksasana.impl.r.ground :as r-ground]))

(defn setup
  ([]
   (r-ground/setup {}))
  ([options]
   (r-ground/setup options)))
