(ns vrksasana.season
  (:require [vrksasana.ground :as ground]
            [vrksasana.catalog :as catalog]
            [clojisr.v1.protocols]))

(defprotocol PSeason
  (eval-code [this code])
  (string-to-print [this fruit])
  (ground [this])
  (season-name [this])
  (attributes [this])
  (close [this])
  (fruit-value->data [this fruit-value])
  (data->fruit-value [this data]))

(defn start [ground season-name]
  (let [season (->> season-name
                    (catalog/season-attributres-by-name)
                    (ground/new-season ground season-name))]
    (catalog/add-season season-name season)
    season))

(defn end [season-name]
  (catalog/remove-season season-name))

(defn get-or-make [ground season-name]
  (or (catalog/get-season season-name)
      (start ground season-name)))

(defn current-season [ground]
  (->> ground
       catalog/current-season-name
       (get-or-make ground)))

