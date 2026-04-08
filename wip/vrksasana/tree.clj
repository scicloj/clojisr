(ns vrksasana.tree
  (:require [vrksasana.util :as util]
            [vrksasana.catalog :as catalog]))

(defrecord Tree [tree-name ast])

(defn ->tree
  ([ast]
   (->tree nil ast))
  ([tree-name ast]
   (let [tree-name (or tree-name (util/rand-name))
         tree (->Tree tree-name ast)]
     (catalog/add-tree-when-missing tree)
     tree)))
