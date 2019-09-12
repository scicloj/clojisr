(ns clojuress.packages.rmarkdown
  (:require [clojuress :as r :refer [r]]
            [clojuress.util :refer [starts-with?]]
            [hiccup.core :as hiccup]
            [tech.resource :as resource]
            [clojure.string :as string]
            [clojure.java.browse :as browse]
            [gg4clj.core :as gg]
            [clojure.walk :as walk]
            [tech.ml.dataset :as dataset])
  (:import (java.io File)
           (java.lang Math)))

(defn r-code-block [r-codes]
  (println [:r-codes r-codes])
  (->> r-codes
       (string/join "\n")
       (format "\n```{r}\n%s\n```\n")))

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


(comment

  (let [x (repeatedly 1000 rand)
        y (map +
               x
               (repeatedly rand))
        data {:x x
              :y y}]
    (-> [:body
        {:style "background-color:#aaaaaa"}
        [:div
         [:h1 "hi!"]
         [:r-edn
          [:library :ggplot2]
          [:qplot :x :y]]]]
       hiccup->rmd
       (render-rmd data)
       browse/browse-url))


  (let [x    (repeatedly 1000 rand)
        y    (map +
                  x
                  (repeatedly rand))
        data {:df (dataset/name-values-seq->dataset
                   {:x x
                    :y y})}]
    (-> [:body
         {:style "background-color:#aaaaaa"}
         [:div
          [:h1 "hi!"]
          [:r-edn
           [:library :ggplot2]
           [:head :df]
           (gg/r+
            [:ggplot {:data :df}]
            [:geom_point [:aes {:x :x :y :y}]])]]]
        hiccup->rmd
        (render-rmd data)
        browse/browse-url))



  (let [x    (repeatedly 9999 rand)
        y    (->> x
                  (map #(+ (* %
                              (Math/sin (* 99 %))
                              (Math/tan %))
                           (rand))))
        z    (->> x
                  (map #(* % (Math/cos (* 9 %)))))
        data {:df (dataset/name-values-seq->dataset
                   {:x x
                    :y y
                    :z z})}]
    (-> [:body
         {:style "background-color:#aaaaaa"}
         (into
          [:div]
          (for [n (->> (range 7)
                       (map #(Math/round
                              (Math/pow 4 %))))]
            [:div
             [:h1 n " samples"]
             [:r-edn
              [:library :ggplot2]
              [:head :df]
              (gg/r+
               [:ggplot {:data [:head :df n]}]
               [:geom_point [:aes {:x :x
                                   :y :y
                                   :color :z}]])]]))]
        hiccup->rmd
        (render-rmd data)
        browse/browse-url))


)
