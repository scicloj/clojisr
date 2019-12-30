(ns clojuress.v0.packages.rmarkdown
  (:require [clojuress.v0 :as r :refer [r add-package-to-this-ns]]
            [clojuress.v0.util :refer [starts-with?]]
            [hiccup.core :as hiccup]
            [tech.resource :as resource]
            [clojure.string :as string]
            [gg4clj.core :as gg]
            [clojure.walk :as walk])
  (:import (java.io File)
           (java.lang Math)))

(add-package-to-this-ns 'rmarkdown)

(defn r-code-block [r-codes]
  (println [:r-codes r-codes])
  (->> r-codes
       (string/join "\n")
       (format "\n```{r echo=F, warning=F, message=F}\n%s\n```\n")))

(defn r-edn-code-block [r-edn-codes]
  (->> r-edn-codes
       (map gg/to-r)
       r-code-block))

(defn hiccup->rmd [hiccup]
  (->> hiccup
       (walk/postwalk
        (fn [form]
          (cond (starts-with? form :r)
                (-> form
                    rest
                    r-code-block)
                (starts-with? form :r-edn)
                (-> form
                    rest
                    r-edn-code-block)
                :else
                form)))
       hiccup/html))

(defn render-rmd [rmd data]
  (let [rmd-file  (File/createTempFile "doc" ".Rmd")
        rmd-path  (.getAbsolutePath rmd-file)
        html-path (string/replace rmd-path
                                  #"\.Rmd"
                                  ".html")
        html-file (File. html-path)]
    (resource/stack-resource-context
     (resource/track #(.delete rmd-file))
     (->> rmd
          (spit rmd-path))
     (r/apply-function
      (r "function(rmd, data) with(data, rmarkdown::render(rmd))")
      [rmd-path
       data])
     html-path)))


