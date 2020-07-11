(ns vrksasana.core
  (:require [vrksasana.catalog :as catalog]
            [vrksasana.season :as season]
            [vrksasana.ground :as ground]
            [vrksasana.tree :as tree]
            [vrksasana.fruit :as fruit]
            [vrksasana.ast :as ast]))

(defn start []
  (catalog/reset))

(defn end []
  (doseq [season (catalog/seasons)]
    (season/end season)))

(defn setup-ground
  ([ground]
   (setup-ground ground {}))
  ([ground
    {:keys [make-default]
     :or   {make-default true}}]
   (let [nam (ground/ground-name ground)]
     (catalog/add-ground nam ground)
     (when make-default
       (catalog/set-default-ground-name nam)))))

(defn restart
  ([]
   (restart nil))
  ([& setup-args]
   (end)
   (start)
   (when setup-args
     (apply setup-ground setup-args))))

(defn ground-to-use
  ([]
   (ground-to-use {}))
  ([{:keys [ground]}]
   (or ground
       (catalog/default-ground))))

(defn season-to-use
  ([]
   (season-to-use {}))
  ([{:keys [season] :as options}]
  (or season
      (-> options
          ground-to-use
          (season/current-season)))))

(defn fruit->data [fruit]
  (let [fresh-fruit (fruit/get-fresh fruit)]
    (season/fruit-value->data
     (:season fresh-fruit)
     (:value fresh-fruit))))

(defn data->fruit
  ([data]
   (data->fruit data nil nil))
  ([data predefined-tree options]
   (let [season (season-to-use options)
         ground (season/ground season)
         tree   (or predefined-tree
                    (-> data
                        ast/->data-dep-ast
                        tree/->tree))
         varname (->> tree
                      :tree-name
                      (ground/tree-name->var-name ground))]
     (->> data
          (season/data->fruit-value season varname)
          (fruit/->Fruit season tree)))))

(defn plant
  ([seedling]
   (plant seedling nil))
  ([seedling {:keys [tree-name] :as options}]
   (let [ground (ground-to-use options)]
     (tree/->tree tree-name
                  (ground/seedling->ast ground seedling)))))

(defn pick
  ([tree]
   (pick tree nil))
  ([tree options]
   (let [season         (season-to-use options)
         ground         (season/ground season)
         refined-options (assoc options :season season)
         var-name (->> tree
                       :tree-name
                       (ground/tree-name->var-name ground))
         ast (:ast tree)]
     (catalog/add-tree-to-season tree season)
     (println [:ast ast])
     (if (ast/data? ast)
       ;; a tree of clojure data
       (-> ast
           second ; the data part
           (data->fruit tree options))
       ;; else -- not a tree of clojure data
       (do
         ;; handle dependencies
         (doseq [dep (ast/tree->deps tree)]
           (pick dep refined-options))
         ;; generate code and evaluate it
         (->> ast
              (ground/ast->code ground)
              (ground/assignment-code ground var-name)
              (season/eval-code season)
              (fruit/->Fruit season tree)))))))

