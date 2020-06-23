(ns clojisr.v1.impl.common
  (:require [clojisr.v1.impl.protocols :as prot]
            [tech.v2.datatype :refer [->reader]]
            [tech.v2.datatype.functional :refer [argfilter]]
            [tech.ml.dataset.column :refer [new-column]]))

(set! *warn-on-reflection* true)

(defn strange-name?
  "Is a given name invalid for a Clojure symbol or keyword?"
  [aname]
  (or (re-matches #"[\Q[](){}#@;:,\/`^'~\"\E].*" aname)
      (-> aname symbol name (not= aname))))

(comment
  (-> "%/%" symbol name)
  ;; => "%"
  (strange-name? "%/%")
  ;; => true
  )

(defn strange-symbol-name?
  [aname]
  (strange-name? aname))


(defn strange-keyword-name? [aname]
  (strange-name? aname))

(defn usually-keyword
  "Given a name in an R named list, turn it into a keyword unless it contains strange characters, but turn it into a string if it does."
  [aname]
  (let [aname-string (name aname)]
    (if (strange-keyword-name? aname-string)
      aname-string
      (keyword aname))))

(defn valid-list-names
  "Create key names for partially named lists"
  [names]
  (map-indexed (fn [^long id k]
                 (if (empty? k) id (usually-keyword k))) names))


;; REXP/SEXP helpers for dataset conversion

(defn ->seq-with-missing
  [xs missing]
  (mapv (fn [d na] (when-not na d)) xs missing))

(defn ->column
  [xs name datatype missing]
  (if datatype
    (new-column name (->reader xs {:datatype datatype}) nil (seq (argfilter identity missing)))
    (new-column name (->reader xs) nil (seq (argfilter identity missing)))))

;; Timeseries

(defn tsp->reader
  "Mathematically better `range`"
  [[^double start ^double stop ^double freq]]
  (let [step-no (int (Math/ceil (* freq (- stop start))))
        step (/ 1.0 freq)]
    (->reader (double-array (for [^long i (range step-no)
                                  :let [curr (+ start (* i step))]
                                  :when (<= curr stop)]
                              curr)))))

;; date-time

(defmacro ^:private reify-dt
  [exp data]
  `(reify
     prot/RProto
     (attribute [_ attr#] (prot/attribute ~exp attr#))
     (attribute-names [_] (prot/attribute-names ~exp))
     (inherits? [_ clss#] (prot/inherits? ~exp clss#))
     (na? [_] (prot/na? ~exp))
     prot/Clojable
     (->clj [_] (deref ~data))
     (->native [_] (prot/->native ~exp))
     prot/DatasetProto
     (->column [_ name#] (->column (deref ~data) name# :local-date-time (prot/na? ~exp)))))

(defn ct-datetime? [obj] (prot/inherits? obj "POSIXct"))
(defn ct-datetime->object
  "CT - seconds from epoch"
  [exp]
  (let [data (delay (mapv (fn [t]
                            (when t (-> (* 1000.0 ^double t)
                                        (java.time.Instant/ofEpochMilli)
                                        (java.time.LocalDateTime/ofInstant (java.time.ZoneId/systemDefault))))) (prot/->clj exp)))]
    (reify-dt exp data)))

(defn lt-datetime? [obj] (prot/inherits? obj "POSIXlt"))
(defn lt-datetime->object
  "LT - list"
  [exp]
  (let [data (delay (->> exp
                         (prot/->clj)
                         ((juxt :year :mon :mday :hour :min :sec))
                         (apply mapv (fn [year month day hour min sec]
                                       (java.time.LocalDateTime/of (int (+ 1900 ^long year))
                                                                   (int (inc ^long month))
                                                                   (int day)
                                                                   (int hour)
                                                                   (int min)
                                                                   (int sec)
                                                                   (int 0))))))]
    (reify-dt exp data)))


;; factor
;; we do this because sometimes REXPFactor is not created

(defn factor? [obj] (and (prot/inherits? obj "factor")
                         (prot/attribute obj "levels")))
(defn factor->clj
  "Create internal factor object"
  [obj]
  (let [levels (-> keyword
                   (map (prot/attribute obj "levels"))
                   (conj nil) ;; align
                   (vec))
        ids (prot/->native obj)
        data (delay (mapv #(get levels %) ids))
        freqs (delay (frequencies @data))]
    (reify
      Object
      (toString [_] (str @data))
      clojure.lang.Seqable
      (seq [_] (seq @data))
      prot/FactorProto
      (levels [_] (rest levels))
      (indexes [_] (seq ids))
      (counts [obj] @freqs)
      prot/RProto
      (attribute [_ attr] (prot/attribute obj attr))
      (attribute-names [_] (prot/attribute-names obj))
      (inherits? [_ clss] (prot/inherits? obj clss))
      (na? [_] (prot/na? obj))
      prot/Clojable
      (->clj [_] @data)
      (->native [_] (prot/->native obj))
      prot/DatasetProto
      (->column [_ name] (->column @data name :keyword (prot/na? obj))))))

(defn first-step->java
  "Perform lower level data conversion"
  [exp]
  (cond
    (factor? exp) (factor->clj exp)
    (ct-datetime? exp) (ct-datetime->object exp)
    (lt-datetime? exp) (lt-datetime->object exp)
    :else exp))

(defn java->column
  [exp name]
  (prot/->column (first-step->java exp) name))
