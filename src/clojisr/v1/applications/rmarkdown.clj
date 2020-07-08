(ns clojisr.v1.applications.rmarkdown
  (:require [clojisr.v1.r :as r :refer [r]]
            [clojisr.v1.util :refer [starts-with? exception-cause]]
            [hiccup.core :as hiccup]
            [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.tools.logging.readable :as log])
  (:import (java.io File)))

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
        ;; html-file (File. html-path)
        ]
    (try
      (->> rmd
           (spit rmd-path))
      (r/apply-function
       (r "function(rmd, data) with(data, rmarkdown::render(rmd))")
       [rmd-path
        data])
      html-path
      (catch Exception e (log/warn [::render-rmd {:message "Can't create html from rmarkdown."
                                                  :exception (exception-cause e)}]))
      (finally (.delete rmd-file)))))


