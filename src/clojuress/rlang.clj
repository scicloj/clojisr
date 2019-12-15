(ns clojuress.rlang
  (:require [clojure.string :as string]
            [clojuress.protocols :as prot]
            [clojuress.util :refer [starts-with?]]
            [tech.resource :as resource]
            [clojuress.robject :refer [->RObject]]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import clojuress.robject.RObject))

(defn- rand-name []
  (-> (java.util.UUID/randomUUID)
      (string/replace "-" "")
      (->> (take 16)
           (cons \x)
           (apply str))))

(defn- object-name->memory-place [obj-name]
  (format ".MEM$%s" obj-name))

(defn- code-that-remembers [obj-name code]
  (format "%s <- {%s}; 'ok'"
          (object-name->memory-place obj-name)
          code))

(defn- code-to-forget [obj-name]
  (format "%s <- NULL; 'ok'"
          (object-name->memory-place obj-name)))

(defn init-session-memory [session]
  (prot/eval-r->java session ".MEM <- list()"))

(defn init-session [session]
  (init-session-memory session)
  session)

(defn forget [obj-name session]
  (when (not (prot/closed? session))
    (let [returned (->> obj-name
                        code-to-forget
                        (prot/eval-r->java session))]
      (assert (->> returned
                   (prot/java->clj session)
                   (= ["ok"]))))))

(defn eval-r [code session]
  (let [obj-name (rand-name)
        returned    (->> code
                         (code-that-remembers obj-name)
                         (prot/eval-r->java session))]
    (assert (->> returned
                 (prot/java->clj session)
                 (= ["ok"])))
    (-> (->RObject obj-name session code)
        (resource/track
         #(do (println [:releasing obj-name])
              (forget obj-name session))
         :gc))))

(defn java->r-specified-type [java-object type session]
  (prot/java->specified-type session java-object type))

(defn r-function-on-obj [{:keys [session] :as r-object}
                         function-name return-type]
  (->> r-object
       :object-name
       object-name->memory-place
       (format "%s(%s)" function-name)
       (prot/eval-r->java session)
       (#(prot/java->specified-type session % return-type))))

(defn r-class [r-object]
  (vec
   (r-function-on-obj
    r-object "class" :strings)))

(defn names [r-object]
  (vec
   (r-function-on-obj
    r-object "names" :strings)))

(defn shape [r-object]
  (vec
   (r-function-on-obj
    r-object "dim" :ints)))

(defn r->java [{:keys [session] :as r-object}]
  (->> r-object
       :object-name
       object-name->memory-place
       (prot/get-r->java session)))

(defn java->r [java-object session]
  (if (instance? RObject java-object)
    java-object
    (let [obj-name (rand-name)]
      (prot/java->r-set session
                        (object-name->memory-place
                         obj-name)
                        java-object)
      (->RObject obj-name session nil))))

(defn r-function->r-code [r-function]
  (if (symbol? r-function)
    (name r-function)
    (-> r-function
        :object-name
        object-name->memory-place)))

(declare args->r-code)

(def binary-operators
  '#{+ - / * & && | || == != <= >= < >})

(defn form->r-code [form session]
  (cond (seq? form) (cond
                      ;; a function declaration
                      (-> form first (= 'function))
                      (let [[_ [& arg-symbols] & body] form]
                        (format
                         "function(%s) {%s}"
                         (->> arg-symbols
                              (map name)
                              (string/join ", "))
                         (->> body
                              (map #(form->r-code % session))
                              (string/join "; "))))
                      ;; a ~ formula
                      (-> form first (= 'tilde))
                      (let [[_ lhs rhs] form]
                        (->> [lhs rhs]
                             (map #(form->r-code % session))
                             (apply format "%s ~ %s")))
                      ;; else -- a function call
                      :else
                      (let [[r-function & args] form]
                        (if (binary-operators r-function)
                          (->> args
                               (map #(form->r-code % session))
                               (interleave (repeat (name r-function)))
                               rest
                               (string/join " "))
                          ;; else
                          (format
                           "%s(%s)"
                           (r-function->r-code r-function)
                           (args->r-code args session)))))
        (symbol? form) (name form)
        :else
        (-> form
            (->> (prot/clj->java session))
            (java->r session)
            :object-name
            object-name->memory-place)))

(defn arg->arg-name-and-value [arg]
  (if (starts-with? arg :=)
    (rest arg)
    [nil arg]))

(defn arg-name-and-value->r-code [[arg-name value] session]
  (str (when arg-name
         (str (name arg-name) "="))
       (form->r-code value session)))

(defn mark-named-args [result args]
  (if (empty? args)
    result
    (if (keyword? (first args))
      (recur (conj result [:= (first args) (second args)])
             (rest (rest args)))
      (recur (conj result (first args))
             (rest args)))))

(defn args->r-code [args session]
  (->> args
       (mark-named-args [])
       (map (fn [arg]
              (-> arg
                  arg->arg-name-and-value
                  (arg-name-and-value->r-code session))))
       (string/join ", ")))

(defn apply-function [r-function
                      args
                      session]
  (-> r-function
      list
      (concat args)
      (form->r-code session)
      (eval-r session)))

(defn eval-form [form session]
  (-> form
      (form->r-code session)
      (eval-r session)))


