(ns vrksasana.ast
  (:require [vrksasana.catalog]
            [vrksasana.tree :as tree]))

(defn ast?
  "Is a given form an AST?"
  [form]
  (and (vector? form)
       (-> form first keyword?)
       (-> form first namespace (= "ast"))))

(defn dep?
  "Does a given AST describe a dependency on another tree?"
  [ast]
  (and (vector? ast)
       (-> ast first (= :ast/dep))))

;; TODO: Use tail recursion here.
(defn ast->deps
  "Find which trees a given AST depends on, transitively."
  [ast]
  (if (dep? ast)
    [(second ast)]
    (->> ast
         rest
         (filter ast?)
         (mapcat ast->deps)
         distinct)))

(defn tree->deps [tree]
  (-> tree
      :ast
      ast->deps))

(defn ->dep-ast
  "Create an AST of a dependency on another tree or fruit."
  [tree-or-fruit]
  (let [tree (or (:tree tree-or-fruit)
                 tree-or-fruit)]
    (vrksasana.catalog/add-tree-when-missing tree)
    [:ast/dep tree]))

(defn data?
  "Does a given AST describe a reference to some Clojure data?"
  [ast]
  (and (vector? ast)
       (-> ast first (= :ast/data))))

(defn ->data-ast
  "Create an AST refering to Clojure data."
  [data]
  [:ast/data data])

(defn ->data-dep-ast
  "Given some Clojure data, create an AST stating the dependency on a tree refering to those data.

  Used for forms which are big Clojure data structures."
  [data]
  (->> data
       ->data-ast
       tree/->tree
       ->dep-ast))
