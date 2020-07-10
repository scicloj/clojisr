(ns vrksasana.fruit
  (:require [vrksasana.season :as season]
            [vrksasana.catalog :as catalog]
            [clojure.pprint :as pp]))

(defrecord Fruit [season tree value])

(defn fresh? [fruit]
  (-> fruit
      :season
      (season/season-name)
      catalog/active-season?))

(defn refresh [fruit]
  (throw (ex-info "not-implemented-yet" {})))

(defn get-fresh [fruit]
  (if (fresh? fruit)
    fruit
    (refresh fruit)))

(defn string-to-print [fruit]
  (let [fresh-fruit (get-fresh fruit)]
    (season/string-to-print (:season fresh-fruit)
                            fresh-fruit)))

;; Overriding print
(defmethod print-method Fruit [fruit ^java.io.Writer w]
  (->> fruit
       string-to-print
       (.write ^java.io.Writer w)))

;; Overriding pprint
(defmethod pp/simple-dispatch Fruit [fruit]
  (->> fruit
       :value
       string-to-print
       println))
