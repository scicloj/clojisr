(ns clojuress.v1.applications.plotting
  (:require [clojuress.v1.require :refer [require-r]])
  (:import [java.io File]))

(require-r '[grDevices :refer [svg png dev.off]]
           '[ggplot2 :refer [ggsave]])

(defn plotting-function->svg [plotting-function]
  (let [tempfile (File/createTempFile "ggplot" ".svg")
        path     (.getPath tempfile)
        _        (svg :filename path)
        _        (plotting-function)
        _        (dev-off)
        result   (slurp path)]
    (.delete tempfile)
    result))

(defn ggplot->svg [ggplot-r-object]
  (let [tempfile (File/createTempFile "ggplot" ".svg")
        _        (-> ggplot-r-object
                     (ggsave :filename (.getPath tempfile)))
        svg      (slurp tempfile)]
    (.delete tempfile)
    svg))
