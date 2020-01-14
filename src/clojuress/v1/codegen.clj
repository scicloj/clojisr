(ns clojuress.v1.codegen
  (:require [clojuress.v1.objects-memory :as mem]
            [clojuress.v1.using-sessions :as using-sessions]
            [clojure.string :as string]
            [clojuress.v1.protocols :as prot]
            [clojuress.v1.util :as util :refer [l]]))

(defn r-object->code [r-object]
  (-> r-object
      :object-name
      mem/object-name->memory-place))

(defn value->code [value session]
  (-> value
      (->> (prot/clj->java session))
      (using-sessions/java->r session)
      r-object->code))

(defn r-function->code [r-function]
  (if (symbol? r-function)
    (name r-function)
    (r-object->code r-function)))

(declare form->code)

(def binary-operators
  '#{$ = <<- <- + - / * & && | || == != <= >= < >})

(defn ->function-def-code [[_ [& arg-symbols] & body] session]
  (format
   "function(%s) {%s}"
   (->> arg-symbols
        (map name)
        (string/join ", "))
   (->> body
        (map #(form->code % session))
        (string/join "; "))))

(defn ->formula-code
  [[_ lhs rhs] session]
  (->> [lhs rhs]
       (map #(form->code % session))
       (apply format "(%s ~ %s)")))

(defn ->binary-funcall-code [[op-symbol & args] session]
  (->> args
       (map #(form->code % session))
       (interleave (repeat (name op-symbol)))
       rest
       (string/join " ")
       (format "(%s)")))

(declare args->code)

(defn ->usual-funcall-code [[r-function & args] session]
  (format
   "%s(%s)"
   (r-function->code r-function)
   (args->code args session)))

(defn ->funcall-code
  [[r-function & args] session]
  (if (binary-operators r-function)
    (->binary-funcall-code (cons r-function args) session)
    (->usual-funcall-code (cons r-function args) session)))

(defn seq-form->code [form session]
  (let [f (first form)]
    (cond
      ;; a function declaration
      (= f 'function)
      (->function-def-code form session)
      ;; a lhs~rhs formula
      (= f 'tilde)
      (->formula-code form session)
      ;; a function call
      (or (symbol? f)
          (using-sessions/function? f))
      (->funcall-code form session)
      ;; else - treat as a value
      :else
      (value->code form session))))

(defn form->code [form session]
  (cond (sequential? form) (seq-form->code form session)
        (symbol? form)     (name form)
        :else              (value->code form session)))

(defn arg->arg-name-and-value [arg]
  (if (util/starts-with? arg :=)
    (rest arg)
    [nil arg]))

(defn arg-name-and-value->code [[arg-name value] session]
  (str (when arg-name
         (str (name arg-name) " = "))
       (form->code value session)))

(defn mark-named-args [args]
  (loop [already-marked []
         remaining-args args]
    (if (empty? remaining-args)
      already-marked
      (if (keyword? (first remaining-args))
        (recur (conj already-marked [:= (first remaining-args) (second remaining-args)])
               (rest (rest remaining-args)))
        (recur (conj already-marked (first remaining-args))
               (rest remaining-args))))))

(defn args->code [args session]
  (->> args
       mark-named-args
       (map (fn [arg]
              (-> arg
                  arg->arg-name-and-value
                  (arg-name-and-value->code session))))
       (string/join ", ")))


