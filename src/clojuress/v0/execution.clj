(ns clojuress.v0.execution
  (:require [clojuress.v0.codegen :as codegen]
            [clojuress.v0.using-sessions :as using-sessions]))

(defn apply-function [r-function
                      args
                      session]
  (-> r-function
      list
      (concat args)
      (codegen/form->code session)
      (using-sessions/eval-r session)))

(comment
  (let [s   (clojuress.v0.using-sessions/fetch-or-make-and-init {})
        x   (-> "3"
                (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    (apply-function sin [x] s)))

(defn eval-form [form session]
  (-> form
      (codegen/form->code session)
      (using-sessions/eval-r session)))

(comment
  (let [s   (clojuress.v0.using-sessions/fetch-or-make-and-init {})
        x   (-> "3"
                (using-sessions/eval-r s))
        sin (-> "sin"
                (using-sessions/eval-r s))]
    (list sin x)))
