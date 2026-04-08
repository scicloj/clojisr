(ns vrksasana.impl.r.codegen
  (:require [clojure.walk :as walk]
            [clojure.string :as string]
            [vrksasana.catalog :as catalog]
            [vrksasana.ground :as ground]
            [clojisr.v1.impl.types :as t]))

(set! *warn-on-reflection* true)

;; working with ASTs

(defn astnode? [subast]
  (and (vector? subast)
       (when-let [f (first subast)]
         (and (keyword? f)
              (-> f namespace some?)))))

(declare ast->code)

(defn double->code
  "Convert double with Infinity/NaN awerness"
  [^double in]
  (cond
    (Double/isFinite in) (str in)
    (Double/isNaN in)    "NaN"
    (pos? in)            "Inf"
    :else                "-Inf"))

(defn astnode->code [[nodekind
                      & [body-first & body-rest :as body]]]
  (let [ground (catalog/ground-name->ground :r)]
    (case (name nodekind)
      "funcall"          (->> body-rest
                              first
                              (map ast->code)
                              (string/join ",")
                              (format "%s(%s)" (ast->code body-first)))
      "binary-funcall"   (->> body-rest
                              first
                              (map ast->code)
                              (string/join (ast->code body-first)))
      "unary-funcall"    (->> body
                              (take 2)
                              (map ast->code)
                              ((fn [[f a]]
                                 (format "%s%s" f a))))
      "string"           (format "\"%s\"" body-first) ;; wrapping with double quotes
      "integer"          (str body-first "L")
      "number"           (double->code body-first)
      "named-arg"        (format "%s=%s"
                                 body-first
                                 (-> body second ast->code))
      ;; date/time just as string, to be converted to time by the user
      "datetime"         (format "'%s'" (t/->str body-first))
      "temporal"         (format "'%s'" (t/->str body-first))
      "regular-symbol"   (ast->code body-first)
      "qualified-symbol" (str (ast->code body-first)
                              "::"
                              (ast->code (first body-rest)))
      "backtick"         (str "`" body-first "`")
      "block"            (->> body-first
                              (map ast->code)
                              (string/join ";")
                              (format "{%s}"))
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


