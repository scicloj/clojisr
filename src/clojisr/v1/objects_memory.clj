(ns clojisr.v1.objects-memory
  (:require [clojisr.v1.protocols :as prot]))

(defn object-name->memory-place [obj-name]
  (format ".MEM$%s" obj-name))

(defn code-that-remembers [obj-name code]
  (format "%s <- {%s}; 'ok'"
          (object-name->memory-place obj-name)
          code))

(defn code-to-forget [obj-name]
  (let [mem (object-name->memory-place obj-name)]
    (format "%s <- NULL; rm(%s); 'ok'" mem mem)))

(def init-session-memory-code
  ".MEM <- new.env()")

;; Try to clean memory, there can be no session or session can be ruined in certain ways (like killed processes).
(defn forget [obj-name session]
  (when (not (prot/closed? session))
    (try
      (->> obj-name
           code-to-forget
           (prot/eval-r->java session))
      (catch Exception e))))
