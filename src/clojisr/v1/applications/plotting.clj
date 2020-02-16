(ns clojisr.v1.applications.plotting
  (:require [clojisr.v1.r :refer [r]]
            [clojisr.v1.require :refer [require-r]]
            [cambium.core :as log])
  (:import [java.io File]
           [clojisr.v1.robject RObject]
           [java.awt Graphics2D Image]
           [java.awt.image BufferedImage]
           [javax.swing ImageIcon]))

(require-r '[grDevices :as dev])

(def ^:private files->fns (let [devices (select-keys (ns-publics 'dev) '[pdf png svg jpeg tiff bmp])]
                            (if-let [jpg (get devices 'jpeg)]
                              (assoc devices 'jpg jpg)
                              devices)))

(def ^:private r-print (r "print")) ;; avoid importing `base` here

(defn plot->file
  [filename plotting-function-or-object & device-params]
  (let [apath (.getAbsolutePath (File. filename))
        extension (symbol (second (re-find #"\.(\w+)$" apath)))
        device (files->fns extension)]
    (assert (contains? files->fns extension) (format "%s filetype is not supported!" (name extension)))
    (try
      (apply device apath device-params)
      (try
        (if (instance? RObject plotting-function-or-object)
          (r-print plotting-function-or-object)
          (plotting-function-or-object))
        (catch Exception e (log/warn "Evaluation of plotting function failed."))
        (finally (dev/dev-off)))
      (catch Exception e (log/warn (format "File creation (%s) failed" apath))))))

(defn plot->svg [plotting-function-or-object & svg-params]
  (let [tempfile (File/createTempFile "clojisr_plot" ".svg")
        path     (.getAbsolutePath tempfile)]
    (apply plot->file path plotting-function-or-object svg-params)
    (let [result (slurp path)]
      (.delete tempfile)
      result)))

(defn- force-argb-image
  "Create ARGB buffered image from given image."
  [^Image img]
  (let [^BufferedImage bimg (BufferedImage. (.getWidth img nil) (.getHeight img nil) BufferedImage/TYPE_INT_ARGB)
        ^Graphics2D gr (.createGraphics bimg)]
    (.drawImage gr img 0 0 nil)
    (.dispose gr)
    (.flush img)
    bimg))

(defn plot->buffered-image [plotting-function-or-object & png-params]
  (let [tempfile (File/createTempFile "clojisr_plot" ".png")
        path     (.getAbsolutePath tempfile)]
    (apply plot->file path plotting-function-or-object png-params)
    (let [result (force-argb-image (.getImage (ImageIcon. path)))]
      (.delete tempfile)
      result)))
