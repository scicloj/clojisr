(ns clojuress.rlang.core
  (:require [clojure.string :as string]
            [clojuress.protocols :as prot]
            [clojuress.util :refer [starts-with?]]))

(defrecord RObject [object-name session])

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

(defn init-session-memory [session]
  (prot/eval-r->java session ".memory <- list()"))

(defn init-session [session]
  (init-session-memory session)
  session)

(defn eval-r [r-code session]
  (let [obj-name (rand-name)
        returned    (->> r-code
                         (r-code-that-remembers obj-name)
                         (prot/eval-r->java session))]
    (assert (-> (prot/java->specified-type session returned :strings)
                vec
                (= ["ok"])))
    (->RObject obj-name session)))


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
  (let [obj-name (rand-name)]
    (prot/java->r-set session
                   (object-name->memory-place
                    obj-name)
                   java-object)
    (->RObject obj-name session)))

(defn apply-function [r-function
                      r-args
                      session]
  (let [code
        (format
         "%s(%s)"
         (-> r-function
             :object-name
             object-name->memory-place)
         (->> r-args
              (map (fn [arg]
                     (let [[arg-name arg-value]
                           (if (starts-with? arg :=)
                             (rest arg)
                             [nil arg])]
                       (str (when arg-name
                              (str (name arg-name) "="))
                            (-> arg-value
                                (->> (prot/clj->java session))
                                (java->r session)
                                :object-name
                                object-name->memory-place)))))
              (string/join ", ")))]
    (eval-r code session)))
