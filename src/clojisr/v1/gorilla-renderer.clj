(ns clojisr.v1.gorilla-renderer
  "Implement pink-gorilla renderer

   If this namespace is required, then pink-gorilla notebook
   is able to render RObjects properly (by extending the type 
   Renderable for objects of type clojisr.v1.robject.RObject)

   The library [org.pinkgorilla/gorilla-renderable \"3.0.5\"]
   in project.clj is very lightweight and only defines the renderable
   protocol.   
  "
  (:import clojisr.v1.robject.RObject)
  (:require
   [clojisr.v1.refresh :refer [fresh-object?]]
   [clojisr.v1.protocols :refer [print-to-string]]
   [pinkgorilla.ui.gorilla-renderable :refer [Renderable render]]))


(defn- render-session-lost []
  ^:r [:span.r-session-lost "R session lost - cannot display R object!"])

(defn- render-text [r-obj]
  ^:r [:p/text (print-to-string (:session r-obj) r-obj)])


(defn render-r-object [r-obj]
  (if (fresh-object? r-obj)
    (render-text r-obj)
    (render-session-lost)))


; After extending the type RObject, Pinkie Renderer will use 
; render-r-object to render RObjects.
(extend-type clojisr.v1.robject.RObject
  Renderable
  (render [self]
    (render-r-object self)))





