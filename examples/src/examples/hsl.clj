(ns exmples.hsl
  (:require [clojuress :as r :refer [r]]
            [clojuress.packages.rmarkdown :as rmarkdown]
            [clojuress.packages.leaflet :as leaflet]
            [clojuress.packages.base :as base]
            [clojuress.util :refer [lines-reducible fmap -|>]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [jsonista.core :as jsonista]
            [cheshire.core :as cheshire]
            [tech.ml.dataset :as dataset]
            [clojure.java.browse :as browse]
            [gg4clj.core :as gg]
            [clj-time.core]
            [clj-time.coerce]
            [tech.resource :as resource])
  (:import java.util.Date
           java.io.File))

(def data-path
  #_"/workspace/data/helsinki/hfpos/journey-ongoing-vp-20190917-afternoon-.2"
  "/workspace/data/helsinki/hfpos/journey-ongoing-vp-20190924-noon")


#_(defn line->detailed-datum [line]
  (let [sep-idx (-> line
                    (string/index-of " {"))
        topic   (-> line
                    (subs 0 sep-idx)
                    (string/split #"/")
                    (->> (interleave
                          [:prefix
                           :version
                           :journey_type
                           :temporal_type
                           :event_type
                           :transport_mode
                           :operator_id
                           :vehicle_number
                           :route_id
                           :direction_id
                           :headsign
                           :start_time
                           :next_stop
                           :geohash_level
                           :geohash])
                         (apply array-map)))
        payload (-> line
                    (subs (inc sep-idx))
                    (cheshire/parse-string keyword))]
    {:topic   topic
     :payload payload}))


(defn line->datum-digest [line]
  (let [sep-idx
        (-> line
            (string/index-of " {"))
        ;; topic
        #_[prefix version journey_type temporal_type event_type
           transport_mode operator_id vehicle_number route_id direction_id
           headsign start_time next_stop geohash_level geohash]
        [_ _ _ _ _ _ operator_id vehicle_number route_id _ _ _ _ _ _ ]
        (-> line
            (subs 0 sep-idx)
            (string/split #"/"))
        ;; payload
        [lng lat tsi desi] (-> line
                               (subs (inc sep-idx))
                               jsonista/read-value
                               (get "VP")
                               (map ["long" "lat" "tsi" "desi"]))
        datetime (some-> tsi
                         (* 1000)
                         long
                         clj-time.coerce/from-long)]
    {:vehicle_id (str operator_id "-" vehicle_number)
     :line (str operator_id "-" desi)
     :lng lng
     :lat lat
     :time tsi
     :datetime datetime}))

(def data
  (->> data-path
       io/reader
       lines-reducible
       (into
        []
        (comp
         (take 1000000)
         #_(filter (fn [line]
                   (re-find #"2019-09-21T2.:" line)))
         (map (fn [line]
                (-> line
                    line->datum-digest)))))
       delay))

(->> @data count time)

(r/discard-all-sessions)

(let [data-by-id (->> @data
                      (map #(dissoc % :datetime))
                      (group-by :line)
                      (sort-by (fn [[_ rows]]
                                 (count rows)))
                      reverse
                      (take 3)
                      (into {})
                      (fmap dataset/->dataset))]
  (-> [:body
       (into [:div
              [:r-edn
               [:library :leaflet]
               [:library :ggplot2]
               [:library :ggmap]
               [:library :gganimate]
               [:library :ggpubr]]
              [:r "provider<-providers$CartoDB.Positron"]
              [:r "new_shared_data <- function(data) SharedData$new(data)"]]
             (->> data-by-id
                  keys
                  (map
                   (fn [id]
                     [:div
                      [:h2 id]
                      [:r-edn
                       [:<- :data
                        (-|> :data_by_vehicle
                             [(keyword "'[['") id])]
                       (-|> [:ggmap
                             [:get_stamenmap
                              [:c 24.8312, 60.1301, 25.2022, 60.2893]
                              {:maptype "watercolor"}]]
                            [:+ [:geom_point
                             [:aes {:x :lng
                                    :y :lat
                                    ;; :size [:I 1]
                                    }]
                                 {:color "darkblue"
                                  :data :data}]]
                            [:+ [:scale_color_hue
                                 {:l 50 :c 35}]]
                            [:+ [:transition_time :time]]
                            [:+ [:ease_aes "linear" {:interval 0.0002}]]
                            [:+ [:shadow_wake {:wake_length 0.1
                                               :alpha       :FALSE}]])]]))))]
      rmarkdown/hiccup->rmd
      (rmarkdown/render-rmd {:data_by_vehicle
                             data-by-id})
      browse/browse-url))

(def lines-data
  (->> @data
       (map #(dissoc % :datetime))
       (group-by :line)
       (sort-by (fn [[_ rows]]
                  (count rows)))
       reverse
       (take 3)
       (mapcat val)
       dataset/->dataset))




(-> [:body
       (into [:div
              [:r-edn
               [:library :ggplot2]
               [:library :ggmap]
               [:library :magick]]
              [:div
               [:r-edn
                [:<- :p
                 (-|> [:ggmap
                       [:get_stamenmap
                        [:c 24.8312, 60.1301, 25.2022, 60.2893]
                        {:maptype "terrain"}]]
                      [:+ [:geom_point
                           [:aes {:x :lng
                                  :y :lat
                                  :color :line}]
                           {:data  :data}]]
                      [:+ [:scale_color_hue
                           {:l 50 :c 35}]]
                      [:+ [:transition_time :time]]
                      [:+ [:ease_aes "linear" {:interval 0.0002}]]
                      [:+ [:shadow_wake {:wake_length 0.1
                                         :alpha       :FALSE}]])]
                [:anim_save "/tmp/animation.gif"
                 [:animate :p]]
                :p]]])]
      rmarkdown/hiccup->rmd
      (rmarkdown/render-rmd {:data several-lines-data})
      browse/browse-url)




(let [data-by-line (->> @data
                        (group-by :line)
                        (fmap dataset/->dataset))]
  [:body
   (into [:div
          [:r-edn
           [:library :leaflet]]
          (for [line ["bus-550" "bus-560" "metro-M1"]]
            [:r-edn
             [:<- :data
              (-|> :data_by_line
                   [(keyword "'[['") line])]
             (-|> [:leaflet {:data data}]
                  [:add_circles
                   {:lat "lat"
                    :lng "lng"}])])])]
  rmarkdown/hiccup->rmd
  (rmarkdown/render-rmd {:data_by_line data-by-line})
  browse/browse-url)













#_[:transform {:time
               [:as.POSIXct
                :time
                {:origin "1970-01-01"}]}]

#_(-|> :data
       [:leaflet]
       [:addProviderTiles :provider]
       [:addCircles {:radius 5
                     :lng    (keyword "~lng")
                     :lat    (keyword "~lat")}])
