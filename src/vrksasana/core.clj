(ns vrksasana.core
  (:require [vrksasana.catalog :as catalog]
            [vrksasana.season :as season]
            [vrksasana.ground :as ground]
            [vrksasana.tree :as tree]
            [vrksasana.fruit :as fruit]
            [vrksasana.ast :as ast]))

(defn init []
  (catalog/reset))

(defn ground-to-use [context]
  (-> context
      :ground
      (or (catalog/default-ground))))

(defn season-to-use [context]
  (or (:season context)
      (-> context
          ground-to-use
          (season/current-season))))

(defn plant
  ([seedling]
   (plant seedling nil))
  ([seedling context]
   (let [ground (ground-to-use context)]
     (tree/->tree (:tree-name context)
                  (ground/seedling->ast ground seedling)))))

(defn pick
  ([tree]
   (pick tree nil))
  ([tree context]
   (let [season         (season-to-use context)
         ground         (season/ground season)
         refined-context (assoc context :season season)
         var-name (->> tree
                       :tree-name
                       (ground/tree-name->var-name ground))]
     (doseq [dep (ast/tree->deps tree)]
       (pick dep refined-context))
     (catalog/add-tree-to-season tree season)
     (->> tree
          :ast
          (ground/ast->code ground)
          (ground/assignment-code ground var-name)
          (season/eval-code season)
          (fruit/->Fruit season tree)))))


(defn fruit->data [fruit]
  (let [fresh-fruit (fruit/get-fresh fruit)]
    (season/fruit-value->data
     (:season fresh-fruit)
     (:value fresh-fruit))))

(defn data->fruit
  ([data]
   (data->fruit data nil))
  ([data context]
   (let [season (season-to-use context)
         tree (-> data
                  ast/->data-dep-ast
                  tree/->tree)]
     (->> data
          (season/data->fruit-value season)
          (fruit/->Fruit season tree)))))
