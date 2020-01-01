(ns examples.rmarkdown
  (:require [notespace.v0.note :as note :refer [note note-md note-as-md note-hiccup note-as-hiccup note-void]]
            [clojuress.v0.note :refer [note-r r-lines->md]]))

(note-md "This document shows the potential of generating [R Markdown](https://rmarkdown.rstudio.com) from Clojure [Hiccup](https://github.com/weavejester/hiccup), while binding R data to values passed from Clojure.")

(note-void
 (require '[tech.ml.dataset :as dataset]
          '[clojuress.v0.applications.rmarkdown :refer [hiccup->rmd render-rmd]]
          '[gg4clj.core :as gg]))


(note-md "We create some random data.

Then we create a Hiccup structure with a special block of R-as-EDN code,
that is translated by [gg4clj](https://github.com/JonyEpsilon/gg4clj) to R code.

We convert it to R Markdown, taking care of the code block, and then we render that R Markdown wth our data added to the R environment.")

(note-as-hiccup
 (let [;; Create x as a random sequence.
       xs    (repeatedly 1000 rand)
       ;; Create y as x plus noise.
       ys    (map +
                 xs
                 (repeatedly rand))
       ;; Hold them together
       data {:x xs
             :y ys}]
   (->
    ;; A hiccup structure with a special block of R-as-EDN code.
    ;; Note that it expects some x, y in its environment.
    [:body
     {:style "background-color:#aaaaaa"}
     [:div
      [:h1 "hi!"]
      [:r-edn
       [:library :ggplot2]
       [:qplot :x :y]]]]
    ;; Convert it to R Markdown, taking care of the code block.
    hiccup->rmd
    ;; Render the R Markdown to an html file, with our data added to
    ;; the environment.
    (render-rmd data)
    slurp)))

(note-md "Now we'll do the same, but we pass our data as a
[tech.ml.dataset](https://github.com/techascent/tech.ml.dataset) dataset object,
converted to an R data.frame.")

(note-as-hiccup
 ;; A different way to do the same.
 (let [xs    (repeatedly 1000 rand)
       ys    (map +
                 xs
                 (repeatedly rand))
       ;; This time, our data is a tech.ml.dataset dataset object.
       data {:df (dataset/name-values-seq->dataset
                  {:x xs
                   :y ys})}]
   (->
    ;; This time, our R-as-EDN code expects our data as a dataframe
    ;; in the environment.
    [:body
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
    slurp)))

(note-md "Now let us ses a more complicated example,
with a genrateted Hiccup structure, containing a sequence of code blocks.")

(note-as-hiccup
 ;; A more complicated example, with a generated hiccup structure,
 ;; containing a sequence of code blocks.
 (let [xs   (repeatedly 9999 rand)
       ys    (->> xs
                 (map (fn [x]
                        (+ (* x
                              (Math/sin (* 99 x))
                              (Math/tan x))
                           (rand)))))
       zs    (->> xs
                  (map (fn [x]
                         (* x (Math/cos (* 9 x))))))
       data {:df (dataset/name-values-seq->dataset
                  {:x xs
                   :y ys
                   :z zs})}]
   (-> [:body
        {:style "background-color:#aaaaaa"}
        (into
         [:div]
         (for [n (->> (range 7)
                      (map (fn [i]
                             (Math/round
                              (Math/pow 4 i)))))]
           [:div
            [:h1 n " samples"]
            [:r-edn
             [:library :ggplot2]
             [:head :df]
             (gg/r+
              [:ggplot {:data [:head :df n]}]
              [:geom_point [:aes {:x     :x
                                  :y     :y
                                  :color :z}]])]]))]
       hiccup->rmd
       (render-rmd data)
       slurp
       ((fn [html]
         [:div html])))))


(note/render-this-ns!)

