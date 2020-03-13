(ns clojisr.v1.eval
  (:require [clojisr.v1.codegen :as codegen]
            [clojisr.v1.using-sessions :as using-sessions]))

(defn eval-form
  ([form session] (eval-form nil form session))
  ([obj-name form session]
   (let [code (codegen/form->code form session)]
     (using-sessions/eval-code obj-name code session))))

(defn r
  ([code-or-form session] (r nil code-or-form session))
  ([obj-name code-or-form session]
   (cond
     (instance? clojisr.v1.robject.RObject code-or-form) code-or-form
     (string? code-or-form) (using-sessions/eval-code obj-name code-or-form session)
     :else (eval-form obj-name code-or-form session))))
