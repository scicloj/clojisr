(ns clojisr.v1.impl.java-to-clj
  (:require [tech.v3.dataset :as ds]
            [tech.v3.dataset.column :as col]
            [tech.v3.datatype :refer [->reader]]
            [tech.v3.tensor :as dtt]
            [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.common :refer [tsp->reader first-step->java java->column cartesian-product]]
            [tech.v3.datatype :as dtype]
            [ tech.v3.tensor.dimensions :as tdim]))

(set! *unchecked-math* :warn-on-boxed)

;; regular timeseries

(defn timeseries? [obj] (prot/inherits? obj "ts"))
(defn timeseries->dataset
  "Create dataset with two columns:

  * :$id - series id as float
  * :$series - data"
  [exp]
  (let [tsp-seq (-> exp
                    (prot/attribute "tsp")
                    (tsp->reader))]
    (ds/new-dataset [(col/new-column :$time tsp-seq)
                     (java->column exp :$series)])))

;; data.frame

(defn data-frame? [obj] (prot/inherits? obj "data.frame"))
(defn data-frame->dataset
  [exp]
  (let [row-names (when-let [rn (prot/attribute exp "row.names")] ;; TODO: more idiomatic
                    (when (first rn)
                      (col/new-column :$row.names rn)))
        cols (prot/->columns exp)]
    (ds/new-dataset (if row-names
                      (conj cols row-names)
                      cols))))


;; multidimensional

(defn- fix-dims
  "Ensure 2d dimensionality"
  [dims]
  (if (= 1 (count dims))
    (conj dims 1)
    dims))

(defn- fix-dimnames
  "Add names/values for each dimension leaving first dimension untouched (can be `nil`)"
  [dims dimnames]
  (->> (map vector dims (concat dimnames (repeat nil)))
       (map-indexed (fn [^long id [^long d dn]]
                      (if (zero? id)  ;; leave rownames as they are
                        dn
                        (or dn
                            (range 1 (inc d))))))))

(defn- create-additional-columns
  [rest-names rows]
  (let [cnt (count rest-names)]
    (->> rest-names
         (reverse)
         (cartesian-product)
         (map (fn [names]
                (mapcat (fn [^long id n]
                          [(repeat rows n)
                           (keyword (str "$col-" (dec id)))])
                        (range cnt 0 -1)
                        names))))))

(defn- add-additional-columns
  [curr add-cols]
  (apply conj curr add-cols))

(defn- make-nd
  ([exp dims dimnames]
   (make-nd exp nil dims dimnames))
  ([exp extra-cols [^long rows ^long cols] [row-names col-names & rest-names]]
   (let [additional-cols (create-additional-columns rest-names rows)]
     (->> (prot/->clj exp)
          (partition (* rows cols)) ;; split into submatrices
          (map (fn [add-cols matrix]
                 (cond-> (interleave col-names
                                     (partition rows matrix)) ;; split into columns
                   add-cols (add-additional-columns add-cols) ;; add rest columns
                   row-names (conj row-names :$row.names) ;; add row-names if available
                   extra-cols (add-additional-columns extra-cols) ;; maybe there is something else (mts case)
                   :always (->> (apply array-map) ;; convert to datasets
                                (ds/->dataset))))
               (concat additional-cols (repeat nil)))
          (apply ds/concat)))))

(defn multidim? [obj] (prot/attribute obj "dim"))

(defn- make-tensor [exp dims]
  
  (def exp exp)
  (def dims dims)
  
  (dtt/construct-tensor (prot/->clj exp) 
                        (tdim/dimensions dims)))
  ;; (dtt/->tensor [[7 4 6 8 8 7 5 9 7 8]
  ;;                [4 1 3 6 5 2 3 5 4 2]
  ;;                [3 8 5 1 7 9 3 8 5 2]] :datatype :float64))
(defn multidim->dataset-or-tensor
  ([exp] (multidim->dataset-or-tensor exp nil nil))
  ([exp extra] (multidim->dataset-or-tensor exp extra nil))
  ([exp extra options]
   
     
     (let [dims (fix-dims (prot/attribute exp "dim"))
           dimnames (fix-dimnames dims (prot/attribute exp "dimnames"))]
       (if (:as-tensor options)
         (make-tensor exp dims)
         (make-nd exp extra dims dimnames)))))

(defn mts? [obj] (prot/inherits? obj "mts"))
(defn mts->dataset
  [exp]
  (let [tsp (-> exp
                (prot/attribute "tsp")
                (tsp->reader))]
    (multidim->dataset-or-tensor exp [tsp :$time])))


;; table

(defn table? [obj] (or (prot/inherits? obj "table")
                       (map? (prot/attribute obj "dimnames"))))
(defn table->dataset
  "Create dataset with dimensions as columns."
  [exp]
  (let [dimnames (-> exp
                     (prot/attribute "dimnames"))
        dimnames (if-not (map? dimnames) ;; table without column names
                   (->> dimnames
                        (interleave (map (comp keyword (partial str "$col-")) (range)))
                        (apply array-map))
                   dimnames)
        cols (->> dimnames
                  (vals)
                  (reverse)
                  (cartesian-product)
                  (map reverse)
                  (map #(apply array-map (interleave %1 %2)) (repeat (keys dimnames))))]
    (ds/->dataset (map #(assoc %1 :$value %2) cols (prot/->clj exp)))))

;; dist

(defn dist? [obj] (prot/inherits? obj "dist"))
(defn dist->dataset
  [exp]
  (let [labels (prot/attribute exp "Labels")
        cnt (count labels)
        values (prot/->clj exp)
        datatype (if (integer? (first values)) :int64 :float64)
        offsets (vec (reductions (fn [^long i ^long v]
                                   (+ i v)) 0 (range (dec cnt) 1 -1)))
        cols (map-indexed (fn [^long col label]
                            (col/new-column label
                                            (->reader (vec (for [^long row (range cnt)]
                                                             (if (= row col) 0
                                                                 (let [^long off (offsets (if (< col row) col row))]
                                                                   (values (+ off (dec (Math/abs (- row col))))))))) datatype))) labels)]
    (ds/new-dataset (conj cols (col/new-column :$row.names labels)))))

;; two stage conversion to avoid recursive call on several classes

(defn java->clj
  "Perform high level data conversion"
  
  ([exp options]
   ;(println :options options)
   (def options options)
   (let [exp (first-step->java exp)]
     (cond
       (data-frame? exp) (data-frame->dataset exp)
       (mts? exp) (mts->dataset exp)
       (timeseries? exp) (timeseries->dataset exp)
       (dist? exp) (dist->dataset exp)
       (table? exp) (table->dataset exp)
       (multidim? exp) (multidim->dataset-or-tensor exp nil options)
       :else (prot/->clj exp))))
  ( [exp] (java->clj exp nil)))

  (defn java->native
    [exp]
    (prot/->native exp))

