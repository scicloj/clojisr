(ns clojuress.v1.objects-memory
  (:require [clojuress.v1.protocols :as prot]))

(defn object-name->memory-place [obj-name]
  (format ".MEM$%s" obj-name))

(defn code-that-remembers [obj-name code]
  (format "%s <- {%s}; 'ok'"
          (object-name->memory-place obj-name)
          code))

(defn code-to-forget [obj-name]
  (format "%s <- NULL; 'ok'"
          (object-name->memory-place obj-name)))

(def init-session-memory-code
  ".MEM <- list()")

(defn forget [obj-name session]
  (when (not (prot/closed? session))
    (let [returned (->> obj-name
                        code-to-forget
                        (prot/eval-r->java session))]
      (assert (->> returned
                   (prot/java->clj session)
                   (= ["ok"]))))))

