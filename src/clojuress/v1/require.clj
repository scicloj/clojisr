(ns clojuress.v1.require
  (:require [clojuress.v1.session :as session]
            [clojuress.v1.functions :as functions]
            [clojuress.v1.eval :as evl]
            [clojuress.v1.using-sessions :as using-sessions]
            [clojuress.v1.protocols :as prot]
            [clojuress.v1.util :as util
             :refer [l clojurize-r-symbol]]))

(defn package-function
  [package-symbol function-symbol]
  (let [delayed (delay (functions/function
                        (evl/r (format "{%s::`%s`}"
                                       (name package-symbol)
                                       (name function-symbol))
                               (session/fetch-or-make nil))))]
    (fn [& args]
      (apply @delayed args))))

(defn package-symbol->all-functions-symbols [package-symbol]
  (let [session (session/fetch-or-make nil)]
    (->> package-symbol
         name
         vector
         (#(functions/apply-function
            (evl/r "function(package_name) as.character(unlist(lsf.str(paste0('package:', package_name))))"
                   session)
            %
            session))
         using-sessions/r->java
         (prot/java->clj session)
         (filter (fn [function-name]
                   (re-matches #"[A-Za-z][A-Za-z\\.\\_].*" function-name)))
         (map symbol))))

(defn find-or-create-ns [ns-symbol]
 (or (find-ns ns-symbol)
     (create-ns ns-symbol)))

(defn add-function-to-ns [ns-symbol package-symbol function-symbol]
  (intern ns-symbol
          (clojurize-r-symbol function-symbol)
          (package-function package-symbol function-symbol)))

(defn ->this-ns-symbol []
  (-> *ns* str symbol))

(defn require-r-package [[package-symbol
                          & {:keys [as refer]}]]
  (let [session (session/fetch-or-make nil)]
    (evl/eval-form (l 'library
                      package-symbol)
                   session))
  (let [r-ns-symbol (->> package-symbol
                         (str "r.")
                         symbol)
        this-ns-symbol (->this-ns-symbol)
        function-symbols (package-symbol->all-functions-symbols
                          package-symbol)]
    (find-or-create-ns r-ns-symbol)
    (doseq [function-symbol function-symbols]
      (add-function-to-ns r-ns-symbol package-symbol function-symbol))
    (when as
      (find-or-create-ns as)
      (doseq [function-symbol function-symbols]
        (add-function-to-ns as package-symbol function-symbol)))
    (when refer
      (doseq [function-symbol refer]
        (add-function-to-ns this-ns-symbol package-symbol function-symbol)))))

(defn require-r [& packages]
  (mapv require-r-package packages))

