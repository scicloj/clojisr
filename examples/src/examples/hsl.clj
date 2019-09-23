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
            [gg4clj.core :as gg])
  (:import java.util.Date))

(def data-path
  "/tmp/journey-ongoing-vp-20190917-afternoon-.2")


(defn line->detailed-datum [line]
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
        [_ _ _ _ _ _ _ vehicle_number _ _ _ _ _ _ _ ]
        (-> line
            (subs 0 sep-idx)
            (string/split #"/"))
        ;; payload
        [long lat tsi] (-> line
                           (subs (inc sep-idx))
                           jsonista/read-value
                           (get "VP")
                           (map ["long" "lat" "tsi"]))]
    {:vehicle-number vehicle_number
     :long long
     :lat lat
     :time tsi}))

(def data
  (->> data-path
       io/reader
       lines-reducible
       (into
        []
        (comp (map (fn [line]
                     (-> line
                         line->datum-digest)))))
       delay))

(-> @data count time)

(let [data-by-vehicle (->> @data
                           (group-by :vehicle-number)
                           (fmap dataset/->dataset))]
  (-> [:body
       {:style "background-color:#aaaaaa"}
       (into [:div
              [:r-edn [:library :leaflet]]
              [:r-edn [:library :ggplot2]]
              [:r "provider<-providers$CartoDB.Positron"]
              [:r "new_shared_data <- function(data) SharedData$new(data)"]]
             (->> data-by-vehicle
                  keys
                  (map
                   (fn [vehicle-number]
                     [:div
                      [:h1 "vehicle " vehicle-number]
                      [:r-edn
                       [:<- :data
                        (-|> :data_by_vehicle
                             [(keyword "'[['") vehicle-number]
                             [:transform {:time
                                          [:as.POSIXct
                                           :time
                                           {:origin "1970-01-01"}]}])]
                       [:with :data [:hist :time
                                     {:breaks 30}]]
                       #_(gg/r+ [:ggplot :data]
                              [:geom_histogram {:x :time}])
                       #_(-|> :data
                            [:leaflet]
                            [:addProviderTiles :provider]
                            [:addCircles {:radius 5}])]]))))]
      rmarkdown/hiccup->rmd
      (rmarkdown/render-rmd {:data_by_vehicle
                             data-by-vehicle})
      browse/browse-url))


