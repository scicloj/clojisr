(ns clojuress.v1.require
  (:require [clojuress.v1.session :as session]
            [clojuress.v1.eval :as evl]
            [clojuress.v1.protocols :as prot]
            [clojuress.v1.util :as util
             :refer [clojurize-r-symbol]]
            [clojuress.v1.impl.common
             :refer [strange-symbol-name?]]
            [cambium.core :as log]))

(defn package-r-object [package-symbol object-symbol]
  (evl/r (format "{%s::`%s`}"
                 (name package-symbol)
                 (name object-symbol))
         (session/fetch-or-make nil)))

(defn package-symbol->nonstrange-r-symbols [package-symbol]
  (let [session (session/fetch-or-make nil)]
    (->> (prot/package-symbol->r-symbol-names
          session package-symbol)
         (remove strange-symbol-name?)
         (map symbol))))

(defn all-r-symbols-map [package-symbol]
  (->> package-symbol
       package-symbol->nonstrange-r-symbols
       (map (fn [r-symbol]
              [r-symbol (try
                          (package-r-object package-symbol r-symbol)
                          (catch Exception e
                            (log/warn [::failed-requiring {:package-symbol package-symbol
                                                           :r-symbol r-symbol
                                                           :cause (-> e Throwable->map :cause)}])))]))
       (filter second)
       (into {})))

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
