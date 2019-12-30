(ns clojuress.v0.impl.rserve.clj-to-java
  (:require [clojuress.v0.util :refer [fmap]]
            [tech.ml.dataset :as dataset]
            [tech.ml.protocols.dataset :as ds-prot]
            [tech.v2.datatype.protocols :as dtype-prot]
            [clojuress.v0.robject])
  (:import (org.rosuda.REngine REXP REXPList REXPGenericVector REXPString REXPSymbol REXPLogical REXPDouble REXPInteger REXPLanguage RList REXPNull)
           (java.util List Collection)
           (clojure.lang Named)
           clojuress.v0.robject.RObject
           java.util.Date))


(declare clj->java)

(defn ->rexp-string
  [xs]
  (REXPString.
   (into-array
    (map (fn [x]
           (cond (nil? x)            nil
                 (instance? Named x) (name x)
                 :else               (str x)))
         xs))))

(defn ->rexp-double
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPDouble/NA
                (double x))))
       double-array
       (REXPDouble.)))

(defn ->rexp-integer
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPInteger/NA
                (int x))))
       int-array
       (REXPInteger.)))

(defn ->rexp-factor
  [xs]
  (throw (ex-info "Factors are not supported yet." {:xs xs})))

(defn ->rexp-logical
  [xs]
  (->> xs
       (map (fn [x]
              (if (nil? x)
                REXPLogical/NA
                (if x
                  REXPLogical/TRUE
                  REXPLogical/FALSE))))
       byte-array
       (REXPLogical.)))

(defn ->r-time [xs]
  (-> xs
       (->> (map (fn [^Date d]
                   (some-> d (.getTime)))))
       ->rexp-double
       (.asDoubles)
       (REXPDouble. (REXPList.
                     (RList. [(->rexp-string [""])
                              (->rexp-string ["POSIXct" "POSIXt"])]
                             ["tzone"
                              "class"])))))


(defn primitive-type [obj]
  (cond (nil? obj)              :na
        (integer? obj)          :integer
        (number? obj)           :numeric
        (string? obj)           :character
        (keyword? obj)          :factor
        (inst? obj)             :time
        (instance? Boolean obj) :logical
        :else                   nil))

(def valid-interpretations {:na        [:integer :numeric :character :factor :logical :time]
                            :integer   [:integer :numeric :character]
                            :numeric   [:numeric :character]
                            :character [:character]
                            :factor    [:factor :character]
                            :logical   [:logical :character]
                            :time      [:time]})

(def interpretations-priorities
  (->> valid-interpretations
       (mapcat val)
       frequencies))

(defn finest-primitive-type [sequential]
  (let [n-elements (count sequential)]
    (->> sequential
         (mapcat (fn [elem]
                   (-> elem primitive-type valid-interpretations)))
         frequencies
         (filter (fn [[_ n]]
                   (= n n-elements)))
         (map key)
         (sort-by interpretations-priorities)
         first)))

(def primitive-vector-ctors
  {:integer   ->rexp-integer
   :numeric   ->rexp-double
   :character ->rexp-string
   :factor    ->rexp-factor
   :logical   ->rexp-logical
   :time      ->r-time})

(defn ->primitive-vector [sequential]
  (when-let [primitive-type (finest-primitive-type sequential)]
    ((primitive-vector-ctors primitive-type) sequential)))

(defn ->list [values]
  (->> values
      ;; recursively
      (map clj->java)
      (RList.)
      (REXPGenericVector.)))

(defn ->named-list [amap]
  (-> (RList. (->> amap
                   vals
                   ;; recursively
                   (map clj->java))
              (->> amap
                   keys
                   (map name)))
      (REXPGenericVector.)))

(defn ->data-frame [dataset]
  (->> dataset
       dataset/column-map
       (fmap (comp clj->java ; recursively
                   seq ; TODO: avoid this wasteful conversion
                   dtype-prot/->array-copy))
       ->named-list
       (.asList)
       (REXP/createDataFrame)))


(defn clj->java
  [obj]
  (or (cond
        ;; an r object
        (instance? RObject obj)
        obj
        ;; a java REXP object
        (instance? REXP obj)
        obj
        ;; nil
        (nil? obj)
        (REXPNull.)
        ;; basic types
        (primitive-type obj)
        (clj->java [obj])
        ;; a sequential or array of elements of inferrable primitive type
        (sequential? obj)
        (->primitive-vector obj))
      ;; we get here if ->primitive-vetor returned nil, which means: no inferrable primitive type
      (cond ;; a dataset
        (satisfies? ds-prot/PColumnarDataset obj)
        (->data-frame obj)
        ;; a named list
        (map? obj)
        (->named-list obj)
        ;; an unnamed list
        (sequential? obj)
        (->list obj))))
