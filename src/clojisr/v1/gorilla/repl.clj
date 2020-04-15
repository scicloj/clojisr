(ns clojisr.v1.gorilla.repl
  "require this namespace in order to work with clojisr from 
   a pinkgorilla notebook."
  (:require
   [clojisr.v1.require :refer [require-r]]
   [clojisr.v1.r :as r :refer [r]]
   [clojisr.v1.applications.plotting :refer [plot->file]]
   [clojisr.v1.gorilla.util :refer [fix-svg]]
   
   ; bring renderer to scope, so notebook user does not need two requires
   [clojisr.v1.gorilla.renderer])
  (:import [java.io File]))

(defn pdf-off
  "By default R plots are also being rendered to PDF files.
    To disable this behavior, call no-pdf."
  []
  (require-r '[grDevices])
  (require-r '[grDevices])
  ;(r.grDevices/dev-off)`
  (r "dev.off('pdf')"))

(defn ->svg
  "calls a plotting-function and renders the output as svg in gorilla-notebook"
  [wrapper-params plotting-function-or-object & svg-params]
  (let [tempfile (File/createTempFile "clojisr_notebook_plot" ".svg")
        path     (.getAbsolutePath tempfile)
        {:keys [width height]} wrapper-params]
    (apply plot->file path plotting-function-or-object svg-params)
    (let [result (slurp path)]
      (.delete tempfile)
      ;^:R [:p/html (fix-svg result 300 200)]
      ^:R [:div.clojsrplot (fix-svg result width height)])))

; help returns just file path
; and prints the content to the stdout
; (r "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(mean)))))")
(defn r-doc
  "docstring for R functions
   rfunc: string"
  [rfunc]
  (r (str "capture.output(tools:::Rd2txt(utils:::.getHelpFile(as.character(help(" rfunc ")))))")))


