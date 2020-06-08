(ns clojisr.v1.codegen
  (:require [clojisr.v1.robject]
            [clojisr.v1.astgen :as astgen]
            [clojure.walk :as walk]
            [clojure.string :as string])
  (:import [clojure.lang Named]
           [clojisr.v1.robject RObject]))

;; helpers

;; Convert instant to date/time R string
(defonce ^:private dt-format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

;; Add context to a call, used to change formatting behaviour in certain cases
(defmacro ^:private with-ctx
  [c & r]
  `(let [~'ctx (conj (or ~'ctx #{}) ~@c)] ~@r))

;; Leave nil untouched when coercing seq
(defn ^:private map-with-nil
  [f xs]
  (map #(some-> % f) xs))

;; working with ASTs

(defn astnode? [subast]
  (and (vector? subast)
       (when-let [f (first subast)]
         (and (keyword? f)
              (-> f namespace some?)))))

(declare ast->code)

(defn astnode->code [[nodekind
                      & [body-first & body-rest :as body]] session]

  (let [a->c #(ast->code % session)]
    (case (name nodekind)
      "funcall"          (->> body-rest
                              first
                              (map a->c)
                              (string/join ",")
                              (format "%s(%s)" (a->c body-first)))
      "binary-funcall"   (->> body
                              (take 3)
                              (map a->c)
                              ((fn [[f l r]]
                                 (format "%s%s%s" l f r))))
      "unary-funcall"    (->> body
                              (take 2)
                              (map a->c)
                              ((fn [[f a]]
                                 (format "%s%s" f a))))
      "string"           (format "\"%s\"" body-first) ;; wrapping with double quotes
      "named-arg"        (format "%s=%s"
                                 body-first
                                 (-> body second a->c))
      "r-object"         body-first ; this should be the r-object's name
      "datetime"         (format "'%s'" (.format dt-format body-first))
      "regular-symbol"   (a->c body-first)
      "qualified-symbol" (str (a->c body-first)
                              "::"
                              (a->c (first body-rest)))
      "backtick"         (str "`" body-first "`")
      "block"            (->> body-first
                              (map a->c)
                              (string/join ";"))
      "for-loop"         (->> body
                              (take 3)
                              (map a->c)
                              (apply format "for(%s in %s){%s\n}"))
      "parens"           (->> body-first
                              a->c
                              (format "(%s)"))
      "boolean"          (->> (if body-first
                                "TRUE" "FALSE"))
      "colon" (->> body
                   (take 2)
                   (map a->c)
                   (string/join ":"))
      "empty-arg" ""
      "if-else"  (->> body-first
                      (take 3)
                      (map a->c)
                      ((fn [[pred f1 f2]]
                         (if f2
                           (format "if(%s) {%s} else {%s}"  pred f1 f2)
                           (format "if(%s) {%s}" pred f1)))))
      "while-loop" (format "while(%s) {%s}"
                           (-> body-first a->c)
                           (-> body-rest first a->c))
      "function-def" (format "function(%s) {%s}"
                             (->> body-first
                                  (map a->c)
                                  (string/join ","))
                             (-> body-rest first a->c))
      "formula" (format "(%s~%s)"
                         (-> body-first a->c)
                         (-> body-rest first a->c)))))



(defn ast->code [ast session]
  (->> ast
       (walk/prewalk
        (fn [subast]
          (if (astnode? subast)
            (astnode->code subast session)
            (str subast))))))


(defn form->code
  "Format every possible form to a R string."
  ([form session] (form->code form session #{}))
  ([form session ctx]
   (-> form
       (astgen/form->ast session ctx)
       (ast->code session))))


