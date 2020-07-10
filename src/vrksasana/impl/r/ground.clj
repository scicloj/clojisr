(ns vrksasana.impl.r.ground
  (:require [vrksasana.ground]
            [vrksasana.impl.r.season :as r-season]
            [vrksasana.impl.r.astgen :as r-astgen]
            [vrksasana.impl.r.codegen :as r-codegen]
            [vrksasana.catalog :as catalog]))

(defrecord Ground []
  vrksasana.ground/PGround

  (seedling->ast [this seedling]
    (r-astgen/form->ast seedling))

  (ast->code [this ast]
    (r-codegen/ast->code ast))

  (assignment-code [this var-name code]
    (format "%s <- %s;" var-name code))

  (forgetting-code [this var-name]
    (format "%s <- NULL;" var-name))

  (tree-name->var-name [this tree-name]
    (str ".MEM$" tree-name))

  (ground-name [this] :r)
 
  (default-season-name [this] :r)
 
  (default-season-attributes [this] {})

  (new-season [this season-name attributes]
    (r-season/new-season this season-name attributes)))
