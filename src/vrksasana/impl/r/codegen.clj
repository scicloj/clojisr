(ns vrksasana.impl.r.codegen
  (:require [clojure.walk :as walk]
            [clojure.string :as string]
            [vrksasana.catalog :as catalog]
            [vrksasana.ground :as ground]))

;; helpers

;; Convert instant to date/time R string
(defonce ^:private dt-format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))

;; working with ASTs

(defn astnode? [subast]
  (and (vector? subast)
       (when-let [f (first subast)]
         (and (keyword? f)
              (-> f namespace some?)))))

(declare ast->code)

(defn astnode->code [[nodekind
                      & [body-first & body-rest :as body]]]
  (let [ground (catalog/ground-name->ground :r)]
    (case (name nodekind)
      "funcall"          (->> body-rest
                              first
                              (map ast->code)
                              (string/join ",")
                              (format "%s(%s)" (ast->code body-first)))
      "binary-funcall"   (->> body
                              (take 3)
                              (map ast->code)
                              ((fn [[f l r]]
                                 (format "%s%s%s" l f r))))
      "unary-funcall"    (->> body
                              (take 2)
                              (map ast->code)
                              ((fn [[f a]]
                                 (format "%s%s" f a))))
      "string"           (format "\"%s\"" body-first) ;; wrapping with double quotes
      "named-arg"        (format "%s=%s"
                                 body-first
                                 (-> body second ast->code))
      "datetime"         (format "'%s'" (.format dt-format body-first))
      "regular-symbol"   (ast->code body-first)
      "qualified-symbol" (str (ast->code body-first)
                              "::"
                              (ast->code (first body-rest)))
      "backtick"         (str "`" body-first "`")
      "block"            (->> body-first
                              (map ast->code)
                              (string/join ";"))
      "for-loop"         (->> body
                              (take 3)
                              (map ast->code)
                              (apply format "for(%s in %s){%s\n}"))
      "parens"           (->> body-first
                              ast->code
                              (format "(%s)"))
      "boolean"          (->> (if body-first
                                "TRUE" "FALSE"))
      "colon"            (->> body
                              (take 2)
                              (map ast->code)
                              (string/join ":"))
      "empty-arg"        ""
      "if-else"          (->> body-first
                              (take 3)
                              (map ast->code)
                              ((fn [[pred f1 f2]]
                                 (if f2
                                   (format "if(%s) {%s} else {%s}"  pred f1 f2)
                                   (format "if(%s) {%s}" pred f1)))))
      "while-loop"       (format "while(%s) {%s}"
                                 (-> body-first ast->code)
                                 (-> body-rest first ast->code))
      "function-def"     (format "function(%s) {%s}"
                                 (->> body-first
                                      (map ast->code)
                                      (string/join ","))
                                 (-> body-rest first ast->code))
      "formula"          (format "(%s~%s)"
                                 (-> body-first ast->code)
                                 (-> body-rest first ast->code))
      "dep"              (->> body-first
                              :tree-name
                              (ground/tree-name->var-name ground)))))


(defn ast->code [ast]
  (->> ast
       (walk/prewalk
        (fn [subast]
          (if (astnode? subast)
            (astnode->code subast)
            (str subast))))))


