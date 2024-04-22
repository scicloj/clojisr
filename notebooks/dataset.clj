;; # Something

(ns dataset
  (:require [scicloj.kindly.v4.kind :as kind]
            [nextjournal.clerk :as clerk]
            [clojisr.v1.r :as r]))

(comment (clerk/serve! {:browse? false :watch-paths ["notebooks"]})
         (clerk/show! "notebooks/dataset.clj")
         (clerk/halt!)
         (scittle/show-doc! "notebooks/dataset.clj")
         (clay/start! {:tools [tools/scittle]}))

(require '[clojisr.v1.r :as r :refer [r r->clj clj->r require-r]]
         '[clojisr.v1.printing :as printR])


(require-r '[datasets])

(clerk/html
 [:pre (printR/r-object->string-to-print r.datasets/BOD)])

(clerk/html
 [:pre (printR/r-object->string-to-print (r '(array ~(range 60) :dim [2 5 1 3 2])))])

(def robject-viewer
  {:pred r/r-object?
   :fetch-fn #(clojisr.v1.printing/r-object->string-to-print %2)
   :render-fn '(fn [v] (v/html [:pre v]))})

r.datasets/BOD

(kind/pprint r.datasets/BOD)

(kind/pprint r.datasets/co2)

(kind/pprint r.datasets/lh)

(require '[clojure.test])


(defmacro check
  [value & [pred & args]]
  (let [is-form (if pred
                  `(~pred ~value ~@args)
                  value)
        check-name (-> (gensym "check")
                       (vary-meta assoc
                                  :test `(fn [] (clojure.test/is ~is-form))
                                  :kindly/kind :kind/check))]
    `(def ~check-name
       {:value ~value
        :check ~is-form})))

(check false)
(check 1)
(check true)
(check 1 #{1 2 3})

(r/discard-all-sessions)
