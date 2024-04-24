^{:kindly/hide-code true
  :kindly/kind :kind/hiccup}
[:img
 {:style {:width "100px"}
  :src "https://github.com/scicloj/clojisr/blob/master/resources/ClojisR.svg.png?raw=true"
  :alt "ClojisR logo"}]

;; # Preface

^:kindly/hide-code
(ns index
  (:require [scicloj.kindly.v4.api :as kindly]
            [scicloj.kindly.v4.kind :as kind]
            [clojure.string :as str]))

^:kindly/hide-code
(def md
  (comp kindly/hide-code kind/md))

(md "

Clojure speaks statistics - a [jisr](https://en.wiktionary.org/wiki/جسر) between Clojure and R

**Artifact:** [![(Clojars coordinates)](https://img.shields.io/clojars/v/scicloj/clojisr.svg)](https://clojars.org/scicloj/clojisr)

**Source:** [![(GitHub repo)](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)](https://github.com/scicloj/clojisr)

**Status:** still evolving; not recommended for production yet; already used by several people in their data science explorations

## Existing chapters in this book:
")

^:kindly/hide-code
(defn chapter->title [chapter]
  (or (some->> chapter
               (format "notebooks/clojisr/v1/tutorials/%s.clj")
               slurp
               str/split-lines
               (filter #(re-matches #"^;; # .*" %))
               first
               (#(str/replace % #"^;; # " "")))
      chapter))

(->> "notebooks/chapters.edn"
     slurp
     clojure.edn/read-string
     (map (fn [chapter]
            (prn [chapter (chapter->title chapter)])
            (format "\n- [%s](clojisr.v1.tutorials.%s.html)\n"
                    (chapter->title chapter)
                    chapter)))
     (str/join "\n")
     md)
