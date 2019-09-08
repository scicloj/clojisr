(ns clojuress.r.core
  (:require [clojure.string :as string]
            [clojuress.protocols :as prot]))

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

(defn init-memory [session]
  (prot/evalr->java session ".memory <- list()"))

(defn init [session]
  (init-memory session))

(defn eval [r-code session]
  (let [obj-name (rand-name)
        returned    (->> r-code
                         (r-code-that-remembers obj-name)
                         (prot/evalr->java session))]
    (assert (-> (prot/java->rspecified-type session returned :strings)
                vec
                (= ["ok"])))
    (->RObject obj-name session)))

(defn java->rspecified-type [java-object type session]
  (prot/java->rspecified-type session java-object type))

(defn r-function-on-obj [r-object function-name return-type  session]
  (->> r-object
       :object-name
       object-name->memory-place
       (format "%s(%s)" function-name)
       (prot/evalr->java session)
       (#(prot/java->rspecified-type session % return-type))
       ((case return-type :java->rstrings) session)))

(defn class [r-object session]
  (vec
   (r-function-on-obj
    r-object "class" :strings session)))

(defn names [r-object session]
  (vec
   (r-function-on-obj
    r-object "names" :strings session)))

(defn shape [r-object session]
  (vec
   (r-function-on-obj
    r-object "dim" :ints session)))

(defn r->java [r-object session]
  (->> r-object
       :object-name
       object-name->memory-place
       (prot/getr->java session)))

(defn java->r [java-object session]
  (let [obj-name (rand-name)]
    (prot/java->rset session
                   (object-name->memory-place
                    obj-name)
                   java-object)
    (->RObject obj-name session)))

(defn apply-function [r-function
                      r-args
                      r-named-args
                      session]
  (let [code
        (format
         "%s(%s)"
         (-> r-function
             :object-name
             object-name->memory-place)
         (->> (concat (->> r-args
                           (map (fn [arg]
                                  [nil arg])))
                      r-named-args)
              (map (fn [[arg-name r-object]]
                     (when arg-name
                       (str arg-name "="))
                     (-> r-object
                         :object-name
                         object-name->memory-place)))
              (string/join ", ")))]
    (eval code session)))
