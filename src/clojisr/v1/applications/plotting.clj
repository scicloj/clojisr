(ns ^:no-doc clojisr.v1.applications.plotting
  (:require [clojisr.v1.r :as r :refer [r r->clj rsymbol require-r]]
            [clojisr.v1.util :refer [exception-cause]]
            [clojure.tools.logging.readable :as log]
            [clojure.java.io :as io]
            [scicloj.kindly.v4.kind :as kind])
  (:import [java.io File]
           [clojisr.v1.robject RObject]
           [java.awt Graphics2D Image]
           [java.awt.image BufferedImage]
           [javax.swing ImageIcon]))

(defn- svglite-installed?
  "Check if svglite is available."
  []
  (-> '(%in% "svglite" (rownames (installed.packages)))
      (r)
      (r->clj)
      (first)))

(def files->fns (delay
                (atom (let [_ (require-r '[grDevices])
                            devices (select-keys (ns-publics 'r.grDevices) '[pdf png svg jpeg tiff bmp xfig postscript])
                            svglite? (svglite-installed?)]
                        (when-not svglite?
                          (log/warn [::plotting {:message "We highly recommend installing of `svglite` package."}]))
                        (cond-> devices
                          (devices 'postscript) (assoc 'ps (devices 'postscript))
                          (devices 'jpeg) (assoc 'jpg (devices 'jpeg))
                          svglite? (assoc 'svg (rsymbol "svglite" "svglite")))))))

(defn use-svg!
  "Use from now on build-in svg device for plotting svg."
  []
  (swap! @files->fns assoc 'svg (get (ns-publics 'r.grDevices) 'svg)))

(defn use-svglite!
  "Use from now on svglite device for plotting svg.
  Requires package `svglite` to be installed"
  []
  (swap! @files->fns assoc 'svg (rsymbol "svglite" "svglite")))

(defn plot->file
  [^String filename plotting-function-or-object & device-params]
  (let [r-print (delay (r "print"))
        apath (.getAbsolutePath (File. filename))
        extension (symbol (or (second (re-find #"\.(\w+)$" apath)) :no))
        device (@@files->fns extension)]
    (if-not (contains? @@files->fns extension)
      (log/warn [::plot->file {:message (format "%s filetype is not supported!" (name extension))}])
      (try
        (io/make-parents filename)
        (apply device :filename apath device-params)
        (let [the-plot-robject (try
                                 (if (instance? RObject plotting-function-or-object)
                                   (@r-print plotting-function-or-object)
                                   (plotting-function-or-object))
                                 (catch Exception e
                                   (log/warn [::plot->file {:message   "Evaluation plotting function failed."
                                                            :exception (exception-cause e)}]))
                                 (finally (r "grDevices::dev.off()")))]
          (log/debug [[::plot->file {:message (format "File %s saved." apath)}]])
          the-plot-robject)
        (catch clojure.lang.ExceptionInfo e (throw e))
        (catch Exception e (log/warn [::plot->file {:message (format "File creation (%s) failed" apath)
                                                    :exception (exception-cause e)}]))))))

(defn plot->svg [plotting-function-or-object & svg-params]
  (let [tempfile (File/createTempFile "clojisr_plot" ".svg")
        path     (.getAbsolutePath tempfile)]
    (apply plot->file path plotting-function-or-object svg-params)
    (let [result (slurp path)]
      (.delete tempfile)
      (kind/html result))))

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
