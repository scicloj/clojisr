(ns vrksasana.impl.rserve.ground
  (:require [vrksasana.ground]
            [vrksasana.impl.rserve.season :as rserve-season]
            [vrksasana.impl.rserve.astgen :as rserve-astgen]
            [vrksasana.impl.rserve.codegen :as rserve-codegen]
            [vrksasana.catalog :as catalog]))

(deftype Ground []
  vrksasana.ground/PGround

  (seedling->ast [this seedling]
    (rserve-astgen/form->ast seedling))

  (ast->code [this ast]
    (rserve-codegen/ast->code ast))

  (assignment-code [this var-name code]
    (format "%s <- %s;" var-name code))

  (forgetting-code [this var-name]
    (format "%s <- NULL;" var-name))

  (tree-name->var-name [this tree-name]
    (str ".MEM$" tree-name))
 
  (default-season-name [this]
    :rserve)
 
  (default-season-attributes [this]
    {})

  (new-season [this season-name attributes]
    (rserve-season/new-season this season-name attributes)))

(def ground (->Ground))

(defn init [& {:keys [make-default]}]
  (catalog/add-ground :rserve ground)
  (when make-default
    (catalog/set-default-ground-name :rserve)))
