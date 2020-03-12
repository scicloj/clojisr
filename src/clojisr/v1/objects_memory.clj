(ns clojisr.v1.objects-memory
  (:require [clojisr.v1.protocols :as prot]))

(defn code-that-remembers [obj-name code]
  (format "%s <- {%s}; 'ok'" obj-name code))

(defn code-to-forget [obj-name]
  (format "%s <- NULL; 'ok'" obj-name))

(def ^:const session-env ".MEM")
(def init-session-memory-code (str session-env " <- new.env()"))

;; Try to clean memory, there can be no session or session can be ruined in certain ways (like killed processes).
(defn forget [obj-name session]
  (when (not (prot/closed? session))
    (try
      (->> obj-name
           code-to-forget
           (prot/eval-r->java session))
      (catch Exception e))))
