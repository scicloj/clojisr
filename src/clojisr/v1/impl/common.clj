(ns clojisr.v1.impl.common
  (:require [clojisr.v1.impl.protocols :as prot]
            [tech.v2.datatype :refer [->reader]]
            [tech.v2.datatype.functional :refer [argfilter]]
            [tech.ml.dataset.column :refer [new-column]]))

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

(defn- ->zone-id
  "Return ZoneId object based on string"
  [tz]
  (if-not (#{"" "LMT"} tz) ;; remove old LMT not supported by Java
    (java.time.ZoneId/of tz)
    (java.time.ZoneId/systemDefault)))

(defn- zone-id
  "Extract tzone attribute and convert to ZoneId"
  [exp]
  (-> exp
      (prot/attribute "tzone")
      (first)
      (->zone-id)))

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
     (->column [_ name#] (->column (deref ~data) name# :zoned-date-time (prot/na? ~exp)))))

(defn ct-datetime? [obj] (prot/inherits? obj "POSIXct"))
(defn ct-datetime->object
  "CT - seconds from epoch"
  [exp]
  (let [tz (zone-id exp)
        data (delay (mapv (fn [t]
                            (when t (-> (* 1000.0 ^double t)
                                        (java.time.Instant/ofEpochMilli)
                                        (java.time.ZonedDateTime/ofInstant tz)))) (prot/->clj exp)))]
    (reify-dt exp data)))

(defn lt-datetime? [obj] (prot/inherits? obj "POSIXlt"))
(defn lt-datetime->object
  "LT - list"
  [exp]
  (let [data (delay (->> exp
                         (prot/->clj)
                         ((juxt :year :mon :mday :hour :min :sec :zone))
                         (apply mapv (fn [year month day hour min sec zone]
                                       (java.time.ZonedDateTime/of (+ 1900 ^long year) (inc ^long month) day hour min sec 0 (->zone-id zone))))))]
    (reify-dt exp data)))

(defn first-step->java
  "Perform lower level data conversion"
  [exp]
  (cond
    (ct-datetime? exp) (ct-datetime->object exp)
    (lt-datetime? exp) (lt-datetime->object exp)
    :else exp))

(defn java->column
  [exp name]
  (prot/->column (first-step->java exp) name))
