(ns clojuress.v0.note
  (:require [notespace.v0.note :as note]
            [clojure.walk :as walk]
            [clojuress.v0.r :as r]
            [clojure.string :as string]))

(note/defkind note-r
  :r  {:render-src?    true
       :value-renderer (fn [value]
                         (->> value
                              (walk/postwalk
                               (fn [v1]
                                 (if (r/r-object? v1)
                                   (r/r->clj v1)
                                   v1)))
                              note/value->html))})

(defn r-lines->md
  "Get a sequence of strings, typically corresponding to lines captured
  from the standard output of R functions, and format them as markdown."
 [r-lines]
  (->> r-lines
       r/r->clj
       (string/join "\n")
       (format "```\n%s\n```")))
