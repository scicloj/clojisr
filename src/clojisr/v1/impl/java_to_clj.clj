(ns clojisr.v1.impl.java-to-clj
  (:require [tech.ml.dataset :as ds]
            [tech.ml.dataset.column :as col]

            [clojure.math.combinatorics :refer [cartesian-product]]
            [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.common :refer [tsp->reader first-step->java java->column]]))

(set! *warn-on-reflection* true)
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
         (apply cartesian-product)
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
                                (ds/name-values-seq->dataset))))
               (concat additional-cols (repeat nil)))
          (apply ds/concat)))))

(defn multidim? [obj] (prot/attribute obj "dim"))
(defn multidim->dataset
  ([exp] (multidim->dataset exp nil))
  ([exp extra]
   (let [dims (fix-dims (prot/attribute exp "dim"))
         dimnames (fix-dimnames dims (prot/attribute exp "dimnames"))]
     (make-nd exp extra dims dimnames))))

(defn mts? [obj] (prot/inherits? obj "mts"))
(defn mts->dataset
  [exp]
  (let [tsp (-> exp
                (prot/attribute "tsp")
                (tsp->reader))]
    (multidim->dataset exp [tsp :$time])))


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
                  (apply cartesian-product)
                  (map reverse)
                  (map #(apply array-map (interleave %1 %2)) (repeat (keys dimnames))))]
    (ds/->dataset (map #(assoc %1 :$value %2) cols (prot/->clj exp)))))

;; two stage conversion to avoid recursive call on several classes

(defn java->clj
  "Perform high level data conversion"
  [exp]
  (let [exp (first-step->java exp)]
    (cond
      (data-frame? exp) (data-frame->dataset exp)
      (mts? exp) (mts->dataset exp)
      (timeseries? exp) (timeseries->dataset exp)
      (table? exp) (table->dataset exp)
      (multidim? exp) (multidim->dataset exp)
      :else (prot/->clj exp))))

(defn java->native
  [exp]
  (prot/->native exp))

;;;;;;

(comment

  (r/r "
   day <- c(\"20081101\", \"20081101\", \"20081101\", \"20081101\", \"18081101\", \"20081102\", \"20081102\", \"20081102\", \"20081102\", \"20081103\")
   time <- c(\"01:20:00\", \"06:00:00\", \"12:20:00\", \"17:30:00\", \"21:45:00\", \"01:15:00\", \"06:30:00\", \"12:50:00\", \"20:00:00\", \"01:05:00\")
   dts1 <- paste(day, time)
   dts2 <- as.POSIXct(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts3 <- as.POSIXlt(dts1, format = \"%Y%m%d %H:%M:%S\")
   dts <- data.frame(posixct=dts2, posixlt=dts3)") 
  
  ;; data.frame without row.names
  (java->clj (r/r->java d/BOD))

  ;; data.frame with row.names
  (java->clj (r/r->java d/CO2))

  ;; table
  (java->clj (r/r->java d/UCBAdmissions))

  ;; table without column names
  (java->clj (r/r->java d/crimtab))

  ;; timeseries
  (java->clj (r/r->java d/BJsales))

  ;; matrix with row and column names
  (java->clj (r/r->java d/VADeaths))

  ;; matrix with column names
  (java->clj (r/r->java d/freeny-x))

  ;; 3d array, with names in second and third dimensions
  (java->clj (r/r->java d/iris3))

  ;; 5d array without names
  (java->clj (r/r->java (r/r '(array ~(range 60) :dim [2 5 1 3 2]))))

  ;; multidimensional timeseries
  (java->clj (r/r->java d/EuStockMarkets)) 

  ;; POSIXct vector
  (java->clj (r/r->java (r/r 'dts2)))

  ;; POSIXlt vector
  (java->clj (r/r->java (r/r 'dts3)))

  ;; data.frame
  (java->clj (r/r->java (r/r 'dts)))

  (java->clj (r/r->java d/Harman23-cor))


  (java->clj (r/r->java (r/r '(list :a 11 :b 22 33 44 :e 55 :f 66 77 88 :i 99))))

  ;; 


  #_(into (sorted-map) (map (fn [[k v]]
                              (let [v (var-get v)
                                    c (set (:class v))]
                                (println c)
                                (when-not (or (c "function")
                                              (c "standardGeneric"))
                                  (let [j (r/r->java v)]
                                    [k [(class j) (:class (r/java->clj (._attr j)))]])))) (ns-publics 'r.maps)))


  
  )
