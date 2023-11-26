(ns clojisr.v1.impl.clj-to-java
  (:require [clojisr.v1.impl.protocols :as prot]
            [clojisr.v1.impl.types :as types]
            [tech.v3.dataset.protocols :as ds-prot]
            [tech.v3.dataset.column :as col]
            [tech.v3.dataset :as dataset]
            [tech.v3.datatype :as dtype]
            [tech.v3.dataset :as ds])
  (:import [clojisr.v1.robject RObject]))

(declare clj->java)

;; time

(defn ->r-time
  [session xs]
  (-> xs
      (types/datetimes->doubles)
      (->> (prot/->numeric-vector session))
      (prot/set-attributes! {"tzone" (prot/->string-vector session [""])
                             "class" (prot/->string-vector session ["POSIXct" "POSIXt"])})))

;; dataset

(defn- maybe-primitive-column-or-seq
  [col]
  [(col/column-name col)
   (if (col/missing col)
     (seq col)
     (dtype/->array col))])

(defn ->data-frame
  [session dataset]
  (let [[dataset row-names] (if (dataset/has-column? dataset :$row.names)
                              [(dataset/drop-columns dataset [:$row.names])
                               (prot/->string-vector session (dataset :$row.names))]
                              [dataset nil])
        dataset-map (->> dataset
                         (dataset/columns)
                         (mapcat maybe-primitive-column-or-seq)
                         (apply array-map))
        col-names (prot/->string-vector session (map types/->str (keys dataset-map)))]
    (-> (clj->java session dataset-map)
        (prot/set-attributes! {"class" (prot/->string-vector session ["data.frame"])
                               "names" col-names
                               "row.names" (if row-names
                                             row-names
                                             (prot/->integer-vector session [nil (- (dataset/row-count dataset))]))}))))

;;

(def primitive-vector-ctors
  {:integer   prot/->integer-vector
   :numeric   prot/->numeric-vector
   :character prot/->string-vector
   :factor    prot/->factor
   :logical   prot/->logical-vector
   :time      ->r-time})

(defn ->primitive-vector [session sequential]
  (when-let [primitive-type (types/finest-primitive-r-type sequential)]
    ((primitive-vector-ctors primitive-type) session sequential)))

(defn clj->java
  [session obj]
  (or (cond
        ;; an r object
        (instance? RObject obj) (prot/->symbol session (:object-name obj))

        ;; native rexp or sexp
        (prot/native? session obj) obj
        
        ;; nil
        (nil? obj) (prot/->nil session)

        ;; symbol
        (symbol? obj) (prot/->symbol session (name obj))
        
        ;; basic types
        (types/primitive-r-type obj) ((primitive-vector-ctors (types/primitive-r-type obj)) session [obj])
        
        ;; a sequential or array of elements of inferrable primitive type
        (sequential? obj) (->primitive-vector session obj))
      
      ;; we get here if ->primitive-vetor returned nil, which means: no inferrable primitive type
      (cond
        ;; a dataset
        (ds-prot/is-dataset? obj) (->data-frame session obj)
        
        ;; a map
        (map? obj) (prot/->named-list session
                                      (map types/->str (keys obj))
                                      (map (partial clj->java session) (vals obj)))
        
        ;; a sequential thing with no inferrable primitive type
        (sequential? obj) (prot/->list session (map (partial clj->java session) obj))

        ;; anything else goes as string
        :else (clj->java session (types/->str obj)))))
