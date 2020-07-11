(ns vrksasana.impl.r.astgen
  (:require [vrksasana.util]
            [vrksasana.ast :as ast])
  (:import [clojure.lang Named]))

;; helpers

;; Add context to a call, used to change formatting behaviour in certain cases
(defmacro ^:private with-ctx
  [c & r]
  `(let [~'ctx (conj (or ~'ctx #{}) ~@c)] ~@r))

;; Leave nil untouched when coercing seq
(defn ^:private map-with-nil
  [f xs]
  (map #(some-> % f) xs))

;; all binary operators as set of strings
(def binary-operators #{"$" "=" "<<-" "<-" "+" "-" "/" "*" "&" "&&" "|" "||" "==" "!=" "<=" ">=" "<" ">" "%in%" "%%" "**"})
(def binary-operators-flat #{"=" "<<-" "<-" "$"})
(def unary-operators #{"+" "-"})
(def wrapped-operators #{"+" "-" "/" "*" "&" "&&" "|" "||" "==" "!=" "<=" ">=" "<" ">"})



(declare form->ast)

(defn named-or-anything->string
  "Convert any named or any other object to a string."
  [s]
  (if (instance? Named s)
    (name s)
    (str s)))

(defn ->ast-args
  "Given a seq of arguments, create a sequence of AST nodes for these (partially named) arguments.

  Arguments preceeded by keywords are considered named.

  Used to generate function arguments or when coercing vector to a list."
  ([args ctx] (->ast-args args ctx nil))
  ([args ctx bra?]
   (->> (loop [res []
               [fa & ra :as all] args]
          (if (seq all)
            (if (keyword? fa)
              (recur (conj res [:ast/named-arg
                                (name fa)
                                (form->ast (first ra) ctx)])
                     (rest ra))
              (recur (conj res (if (and bra? (nil? fa))
                                 [:ast/empty-arg]
                                 (form->ast fa ctx)))
                     ra))
            res)))))

(defn ->function-call-ast
  "Create an AST of an R function call."
  [fname args ctx mode]
  [:ast/funcall fname
   (case mode
     :plain args
     :recurse-args (map #(form->ast % ctx) args)
     :prepare-ast-args (->ast-args args ctx))])

(defn ->parens-ast
  "Create an AST that wraps its contents parentheses
  (may matter in some code generation situations)."
  [sub-ast]
  [:ast/parens sub-ast])

(defn binary-or-unary-call->ast
  "Create the AST of a binary or unary operator function call."
  [f [f1 & fr] ctx]
  (let [maybe-wrap (if (:flat ctx)
                     identity
                     ->parens-ast)
        res (if-not f1
              (throw (Exception. "Positive number of arguments is required."))
              (let [f1-ast (form->ast f1 ctx)]
                (if-not fr
                  (if (unary-operators f)
                    [:ast/unary-funcall f f1-ast]
                    f1-ast)
                  (reduce (fn [a1 a2] (maybe-wrap
                                       [:ast/binary-funcall
                                        f
                                        a1
                                        (form->ast a2 ctx)]))
                          f1-ast fr))))]
    (if (and (wrapped-operators f)
             (not (ctx :unwrap))
             (ctx :flat))
      (->parens-ast res)
      res)))

(defn formula->ast
  "Create a formula AST.

  A pair is expected, f the two sides of the formula (left and right).
  If you want to skip given side, use `nil`.

  Formulas are treated as a binary call and are formatted without parentheses
  (context = `:flat`)."
  [[lf rf] ctx]
  (with-ctx [:unwrap :flat]
    [:ast/formula
     (some-> lf (form->ast ctx))
     (some-> rf (form->ast ctx))]))

(defn symbol-form->ast
  "Create the AST of a binary, unary or regular function call.

  Used for forms that are seqs where the first argument is a symbol."
  [f args ctx]
  (if (binary-operators f)
    (if (binary-operators-flat f)
      (with-ctx [:flat] (binary-or-unary-call->ast f args ctx))
      (binary-or-unary-call->ast f args ctx))
    (->function-call-ast f args ctx :prepare-ast-args)))

(defn ->block-ast
  "Create the AST of a block of possibly several expressions."
  [expression-asts]
  [:ast/block expression-asts])

(defn ->function-def-ast
  "Create the AST of an R function definition.

  Arguments can be a partially named list."
  [args body ctx]
  [:ast/function-def
   (->ast-args args ctx)
   (->block-ast (map #(form->ast % ctx) body))])

(defn ->if-else-ast
  "Create the AST of an if or an if-else form."
  [vs ctx]
  [:ast/if-else
   (map #(form->ast % ctx) (take 3 vs))])

(defn ->for-loop-ast
  "Create the AST of a for-loop."
  [bindings body ctx]
  (if (seq bindings)
    (let [[v s & r] bindings]
      [:ast/for-loop
       (name v)
       (form->ast s ctx)
       (->for-loop-ast r body ctx)])
    (->block-ast
     (map #(form->ast % ctx) body))))

(defn ->while-loop-ast
  "Create the AST of a while-loop."
  [pred body ctx]
  [:ast/while-loop
   (form->ast pred ctx)
   (->block-ast
    (map #(form->ast % ctx) body))])

(defn ->colon-ast
  "Create the AST of an R colon (:) expression."
  [[a b] ctx]
  [:ast/colon
   (form->ast a ctx)
   (form->ast b ctx)])

(defn ->bacticks-ast
  "Create the AST of a symbol wrapped with backticks."
  [s]
  [:ast/backtick s])

(defn maybe-wrap-with-backticks
  [string-or-symbol]
  (if (symbol? string-or-symbol)
    (name string-or-symbol)
    (->bacticks-ast (name string-or-symbol))))

(defn ->rsymbol-ast
  "Create the AST of a qualified or regular symbol.

  A regular symbol has one part. A qualified symbol has two"
  ([rsymbol-parts ctx]
   (let [[a b] (->> rsymbol-parts
                    (map (fn [part]
                           (maybe-wrap-with-backticks
                            (if (or (symbol? part) (string? part))
                                 part
                                 (form->ast part ctx))))))]
     (if b
       [:ast/qualified-symbol a b]
       [:ast/regular-symbol a]))))

(defn ->bracket-call-ast
  "Create the AST of a bracket-call [...]."
  [[bra all?] args ctx]
  (let [args (if (and (not all?)
                      (nil? (last args)))
               (conj (vec (butlast args)) 'NULL)
               args)]
    [:ast/funcall
     bra
     (->ast-args args ctx true)]))

(defn ->unquote-form-ast
  "Eval unquoted form.

  Used when unquote symbol is part of regular quoted form.

  Warning: You can't unquote local bindings! For this case use inside syntax quote."
  [u-form ctx]
  (-> (apply eval u-form)
      (form->ast ctx)))

(declare seq-form->ast)

(defn vector->ast
  "Construct the AST corresponding to an R vector (using the `c` function.)

  When the first element is a coersion symbol starting with `:!`, values are coerced to the required type.
  When the number of elements is big enough, the data is converted to an R object AST.

  `nil` is converted to `NA`"
  [[f & r :as v-form] ctx]
  (with-ctx [:na]
    (case f
      :!string (vector->ast (map-with-nil named-or-anything->string r) ctx)
      :!boolean (vector->ast (map-with-nil #(if % true false) r) ctx)
      :!int (vector->ast (map-with-nil unchecked-int r) ctx)
      :!double (vector->ast (map-with-nil unchecked-double r) ctx)
      :!named (->function-call-ast 'c r ctx :prepare-ast-args)
      :!list (->function-call-ast 'list r ctx :prepare-ast-args)
      :!factor (->function-call-ast
                'factor
                [(vector->ast r ctx)]
                ctx :plain)
      :!ct (->function-call-ast
            'as.POSIXct
            [(vector->ast r ctx)]
            ctx :plain)
      :!lt (->function-call-ast
            'as.POSIXlt
            [(vector->ast r ctx)]
            ctx :plain)
      :!call (seq-form->ast r ctx)
      (if (< (count v-form) 80)
        (->function-call-ast
         'c v-form ctx :recurse-args)
        (ast/->data-dep-ast v-form)))))

;; symbol, string, how to process parameters (all or butlast)
(def bracket-data {"bra"      ["`[`" true]
                   "brabra"   ["`[[`" true]
                   "bra<-"    ["`[<-`" false]
                   "brabra<-" ["`[[<-`" false]})

(defn seq-form->ast
  "Process a sequence.

  Possible paths are possible if given first element is:

  * one of the symbols: `function` or `formula` - create fn definition or formula
  * ~ (unquote) - eval rest of the form
  * a tree which is assumed to be a function - create function call
  * sequence - create function call with processed first element.
  * any symbol - function call
  * one of the functions with special names: [,[[,[<-,[[<-,:
  * any other value - construct vector"
  [[f & r :as seq-form] ctx]
  (if (symbol? f)
    (let [fs (name f)]
      (cond
        (= "colon" fs) (->colon-ast r ctx)
        (= "function" fs) (->function-def-ast (first r) (rest r) ctx)
        (or (= "tilde" fs)
            (= "formula" fs)) (formula->ast r ctx)
        (= "rsymbol" fs) (->rsymbol-ast r ctx)
        (= "if" fs) (->if-else-ast r ctx)
        (= "do" fs) (->block-ast (map #(form->ast % ctx) r))
        (= "for" fs) (->for-loop-ast (first r) (rest r) ctx)
        (= "while" fs) (->while-loop-ast (first r) (rest r) ctx)
        (contains? bracket-data fs) (->bracket-call-ast (bracket-data fs) r ctx)
        (= 'clojure.core/unquote f) (->unquote-form-ast r ctx)
        :else (symbol-form->ast fs r ctx)))
    (let [->f-c-ast #(->function-call-ast % r ctx :prepare-ast-args)]
      (cond
        (instance? vrksasana.fruit.Fruit f) (->f-c-ast
                                             (-> f
                                                 :tree
                                                 :tree-name))
        (instance? vrksasana.tree.Tree f) (->f-c-ast
                                           (:tree-name f))
        (string? f)                       (if (and (= (first f) \`)
                                                   (= (last f) \`))
                                            (->f-c-ast f)
                                            (vector->ast seq-form ctx))
        (sequential? f)                   (->f-c-ast (seq-form->ast f ctx))
        :else                             (vector->ast seq-form ctx)))))

(defn map->ast
  "Convert a map to an AST for generatign a named-list.

  For big maps, the data is converted to an R object AST.

  `nil` is treated as `NA`"
  [form ctx]
  (if (< (count form) 50)
    (with-ctx [:na]
      [:ast/funcall
       'list
       (map (fn [[k v]]
              [:ast/named-arg
               (form->ast k ctx)
               (form->ast v ctx)])
            form)])
    (ast/->data-dep-ast form)))

(defn nil->ast
  "Convert `nil` to `NA` or `NULL` (based on context)"
  [ctx]
  (cond
    (ctx :nil) nil
    (ctx :na) "NA"
    :else "NULL"))

(defn form->ast
  "Convert a given form to an AST."
  ([form] (form->ast form #{}))
  ([form ctx]
   (cond
     (vector? form) (vector->ast form ctx) ;; vector always is converted to datatype
     (sequential? form) (seq-form->ast form ctx) ;; sequence is usually call
     (instance? vrksasana.fruit.Fruit form) (-> form
                                                :tree
                                                (form->ast ctx))
     (instance? vrksasana.tree.Tree form) (ast/->dep-ast form)
     (map? form) (map->ast form ctx) ;; map goes to a list
     (string? form) [:ast/string form]
     (integer? form) form ;; int is treated literally
     (rational? form) [:ast/parens form] ;; rational is wrapped in in case of used in calculations
     (number? form) form ;; other numbers are treated literally
     (boolean? form) [:str/boolean form]
     (nil? form) (nil->ast ctx)
     (inst? form) [:ast/datetime form]
     (instance? Named form) (name form)
     :else (ast/->data-dep-ast form))))

