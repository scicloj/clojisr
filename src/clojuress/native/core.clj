(ns clojuress.native.core
  (:require [clojure.string :as string]
            [clojuress.protocols :as prot]))

(defrecord NativeObject [object-name session])


(defn- rand-name []
  (-> (java.util.UUID/randomUUID)
      (string/replace "-" "_")
      (->> (str "x"))))

(defn- object-name->memory-place [obj-name]
  (format ".memory$%s" obj-name))

(defn- r-code-that-remembers [obj-name r-code]
  (format "%s <- {%s}; 'ok'"
          (object-name->memory-place obj-name)
          r-code))

(defn init-memory [session]
  (prot/eval->jvm session ".memory <- list()"))

(defn init [session]
  (init-memory session))

(defn eval [r-code session]
  (let [obj-name (rand-name)
        returned    (->> r-code
                         (r-code-that-remembers obj-name)
                         (prot/eval->jvm session))]
    (assert (-> (prot/jvm->specified-type session returned :strings)
                vec
                (= ["ok"])))
    (->NativeObject obj-name session)))

(defn jvm->specified-type [jvm-object type session]
  (prot/jvm->specified-type session jvm-object type))

(defn native-function-on-obj [native-object function-name return-type  session]
  (->> native-object
       :object-name
       object-name->memory-place
       (format "%s(%s)" function-name)
       (prot/eval->jvm session)
       (#(prot/jvm->specified-type session % return-type))
       ((case return-type :jvm->strings) session)))

(defn class [native-object session]
  (vec
   (native-function-on-obj
    native-object "class" :strings session)))

(defn names [native-object session]
  (vec
   (native-function-on-obj
    native-object "names" :strings session)))

(defn shape [native-object session]
  (vec
   (native-function-on-obj
    native-object "dim" :ints session)))

(defn ->jvm [native-object session]
  (->> native-object
       :object-name
       object-name->memory-place
       (prot/get->jvm session)))

(defn jvm-> [jvm-object session]
  (let [obj-name (rand-name)]
    (prot/jvm->set session
                   (object-name->memory-place
                    obj-name)
                   jvm-object)
    (->NativeObject obj-name session)))

(defn apply-function [native-function
                      native-args
                      native-named-args
                      session]
  (let [code
        (format
         "%s(%s)"
         (-> native-function
             :object-name
             object-name->memory-place)
         (->> (concat (->> native-args
                           (map (fn [arg]
                                  [nil arg])))
                      native-named-args)
              (map (fn [[arg-name native-object]]
                     (when arg-name
                       (str arg-name "="))
                     (-> native-object
                         :object-name
                         object-name->memory-place)))
              (string/join ", ")))]
    (eval code session)))
