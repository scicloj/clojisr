(ns clojuress.v0.eval
  (:require [clojuress.v0.codegen :as codegen]
            [clojuress.v0.using-sessions :as using-sessions]))

(defn eval-form [form session]
  (-> form
      (codegen/form->code session)
      (using-sessions/eval-code session)))

(defn r [code-or-form session]
  (if (string? code-or-form)
    ;; code
    (using-sessions/eval-code code-or-form session)
    ;; form
    (-> code-or-form
        (codegen/form->code session)
        (using-sessions/eval-code session))))
