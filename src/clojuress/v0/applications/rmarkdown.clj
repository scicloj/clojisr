(ns clojuress.v0.applications.rmarkdown
  (:require [clojuress.v0.r :as r :refer [r]]
            [clojuress.v0.util :refer [starts-with?]]
            [hiccup.core :as hiccup]
            [tech.resource :as resource]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [cambium.core :as log])
  (:import (java.io File)
           (java.lang Math)))

(defn r-code-block [r-codes]
  (log/info [::r-codes r-codes])
  (->> r-codes
       (string/join "\n")
       (format "\n```{r echo=F, warning=F, message=F}\n%s\n```\n")))

(defn r-forms-code-block [forms & {:keys [session-args]}]
  (->> forms
       (map (fn [form]
              (r/->code form :session-args session-args)))
       r-code-block))

(defn hiccup->rmd [hiccup]
  (->> hiccup
       (walk/postwalk
        (fn [form]
          (cond (starts-with? form :r)
                (-> form
                    rest
                    r-code-block)
                (starts-with? form :r-forms)
                (-> form
                    rest
                    r-forms-code-block)
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


