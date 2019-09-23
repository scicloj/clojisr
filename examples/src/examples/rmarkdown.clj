(ns examples.rmarkdown
  (:require [tech.ml.dataset :as dataset]
            [clojure.java.browse :as browse]
            [clojuress.packages.rmarkdown :refer [hiccup->rmd render-rmd]]
            [gg4clj.core :as gg]))


(let [;; Create x as a random sequence.
      x    (repeatedly 1000 rand)
      ;; Create y as x plus noise.
      y    (map +
                x
                (repeatedly rand))
      ;; Hold them together
      data {:x x
            :y y}]
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
   ;; Convert it to Rmarkdown, taking care of the code block.
   hiccup->rmd
   ;; Render the Rmarkdown to an html file, with our data added to
   ;; the environment.
   (render-rmd data)
   ;; View the html.
   browse/browse-url))


;; A different way to do the same.

(let [x    (repeatedly 1000 rand)
      y    (map +
                x
                (repeatedly rand))
      ;; This time, our data is a tech.ml.dataset dataset object.
      data {:df (dataset/name-values-seq->dataset
                 {:x x
                  :y y})}]
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
      browse/browse-url))

;; A more complicated example, with a generated hiccup structure,
;; containing a sequence of code blocks.

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
             [:geom_point [:aes {:x     :x
                                 :y     :y
                                 :color :z}]])]]))]
      hiccup->rmd
      (render-rmd data)
      browse/browse-url))



