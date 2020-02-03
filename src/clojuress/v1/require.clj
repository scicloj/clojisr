(ns clojuress.v1.require
  (:require [clojuress.v1.session :as session]
            [clojuress.v1.functions :as functions]
            [clojuress.v1.eval :as evl]
            [clojuress.v1.protocols :as prot]
            [clojuress.v1.util :as util
             :refer [clojurize-r-symbol]]
            [clojuress.v1.impl.common
             :refer [strange-name?]]))

(defn package-r-symbol [package-symbol object-symbol]
  (evl/r (format "{%s::`%s`}"
                 (name package-symbol)
                 (name object-symbol))
         (session/fetch-or-make nil)))

(defn package-function [package-symbol function-symbol]
  (let [delayed (delay (functions/function (package-r-symbol package-symbol function-symbol)))]
    (fn [& args]
      (apply @delayed args))))

(defn package-symbol->nonstrange-r-symbols [package-symbol functions-only?]
  (let [session (session/fetch-or-make nil)]
    (->> (prot/package-symbol->r-symbol-names
          session package-symbol functions-only?)
         (remove strange-name?)
         (map symbol))))

(defn all-r-symbols-map [package-symbol]
  (let [function-symbols (set (package-symbol->nonstrange-r-symbols
                               package-symbol true))]
    (into {} (map (fn [r-symbol]
                    [r-symbol (if (function-symbols r-symbol)
                                (package-function package-symbol r-symbol)
                                (package-r-symbol package-symbol r-symbol))])
                  (package-symbol->nonstrange-r-symbols
                   package-symbol false)))))

(defn find-or-create-ns [ns-symbol]
  (or (find-ns ns-symbol)
      (create-ns ns-symbol)))

(defn add-to-ns [ns-symbol r-symbol r-object]
  (intern ns-symbol
          (clojurize-r-symbol r-symbol)
          r-object))

(defn symbols->add-to-ns [ns-symbol r-symbols]
  (doseq [[r-symbol r-object] r-symbols]
    (add-to-ns ns-symbol r-symbol r-object)))

(defn require-r-package [[package-symbol & {:keys [as refer]}]]
  (let [session (session/fetch-or-make nil)]
    (evl/eval-form ['library
                    package-symbol]
                   session))
  (let [r-ns-symbol (->> package-symbol
                         (str "r.")
                         symbol)
        r-symbols (all-r-symbols-map package-symbol)]

    ;; r.package namespace
    (find-or-create-ns r-ns-symbol)
    (symbols->add-to-ns r-ns-symbol r-symbols)

    ;; alias namespace
    (when as
      (find-or-create-ns as)
      (symbols->add-to-ns as r-symbols))

    ;; inject symbol into current namespace
    (when refer
      (let [this-ns-symbol (-> *ns* str symbol)]
        (symbols->add-to-ns this-ns-symbol
                            (select-keys r-symbols refer))))))

(defn require-r [& packages]
  (run! require-r-package packages))
