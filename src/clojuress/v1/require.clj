(ns clojuress.v1.require
  (:require [clojuress.v1.session :as session]
            [clojuress.v1.functions :as functions]
            [clojuress.v1.eval :as evl]
            [clojuress.v1.using-sessions :as using-sessions]
            [clojuress.v1.protocols :as prot]
            [clojuress.v1.util :as util
             :refer [l clojurize-r-symbol]]))

(defn package-object [package-symbol object-symbol]
  (evl/r (format "{%s::`%s`}"
                 (name package-symbol)
                 (name object-symbol))
         (session/fetch-or-make nil)))

(defn package-function [package-symbol function-symbol]
  (let [delayed (delay (functions/function (package-object package-symbol function-symbol)))]
    (fn [& args]
      (apply @delayed args))))

(defn package-dataset [package-symbol dataset-symbol]
  (package-object package-symbol dataset-symbol))

(defn package-symbol->r-symbols [r-selector-function package-symbol]
  (let [session (session/fetch-or-make nil)]
    (->> package-symbol
         name
         ((fn [package-name] (functions/apply-function
                             (evl/r r-selector-function session)
                             [package-name]
                             session)))
         using-sessions/r->java
         (prot/java->clj session)
         (filter (fn [function-name]
                   (re-matches #"[A-Za-z][A-Za-z\\.\\_].*" function-name)))
         (map symbol)
         (set))))

(def package-symbol->all-functions-symbols
  (partial package-symbol->r-symbols "function(package_name) as.character(unlist(lsf.str(paste0('package:', package_name))))"))

(def package-symbol->all-datasets-symbols
  (partial package-symbol->r-symbols "function(package_name) data(package=package_name)$results[,'Item']"))

(defn find-or-create-ns [ns-symbol]
  (or (find-ns ns-symbol)
      (create-ns ns-symbol)))

(defn add-to-ns [wrapper ns-symbol package-symbol r-symbol]
  (intern ns-symbol
          (clojurize-r-symbol r-symbol)
          (wrapper package-symbol r-symbol)))

(defn symbols->add-to-ns [ns-symbol package-symbol dataset-symbols function-symbols]
  (doseq [function-symbol function-symbols]
    (add-to-ns package-function ns-symbol package-symbol function-symbol))
  (doseq [dataset-symbol dataset-symbols]
    (add-to-ns package-dataset ns-symbol package-symbol dataset-symbol)))

(defn require-r-package [[package-symbol & {:keys [as refer]}]]
  (let [session (session/fetch-or-make nil)]
    (evl/eval-form (l 'library
                      package-symbol)
                   session))
  (let [r-ns-symbol (->> package-symbol
                         (str "r.")
                         symbol)
        dataset-symbols (package-symbol->all-datasets-symbols package-symbol)
        function-symbols (package-symbol->all-functions-symbols package-symbol)]

    ;; r.package namespace
    (find-or-create-ns r-ns-symbol)
    (symbols->add-to-ns r-ns-symbol package-symbol dataset-symbols function-symbols)

    ;; alias namespace
    (when as
      (find-or-create-ns as)
      (symbols->add-to-ns as package-symbol dataset-symbols function-symbols))

    ;; inject symbol into current namespace
    (when refer
      (let [refer-set (set refer)
            this-ns-symbol (-> *ns* str symbol)]
        (symbols->add-to-ns this-ns-symbol package-symbol
                            (clojure.set/intersection refer-set dataset-symbols)
                            (clojure.set/intersection refer-set function-symbols))))))

(defn require-r [& packages]
  (run! require-r-package packages))
