
(ns clojisr.v1.gorilla-util
  (:require
   [clojure.string :as str]
   [clojure.walk :refer [prewalk]]
   [pl.danieljanus.tagsoup :as ts]
   [com.rpl.specter :refer [transform ALL]]
   ))

;; Hiccup accepts Style as string, but Reagent does not.
;; Example: [:rect {:width "100%", :height "100%", :style "stroke: none; fill: #FFFFFF;"}]  

(defn to-map-style [s]
  (let [style-vec (map #(str/split % #":") (str/split s #";"))]
    (into {}
          (for [[k v] style-vec]
            [(keyword (str/trim k)) (str/trim v)]))))

(defn is-style? [x]
  ;(println "is-style? " x)
  (if (and (vector? x)
           (= 2 (count x))
           (= (first x) :style)
           (string? (second x)))
    true
    false))

(defn replace-style [x]
  (into [] (assoc x 1 (to-map-style (last x)))))

(defn convert-style-as-strings-to-map
  "resolve function-as symbol to function references in the reagent-hickup-map.
   Leaves regular hiccup data unchanged."
  [hiccup-vector]
  (prewalk
   (fn [x]
     (if (is-style? x)
       (replace-style x)
       x))
   hiccup-vector))


(defn is-tag? [tag x]
  ;(println "is-style? " x)
  (if (and (vector? x)
           (> 1 (count x))
           (= (first x) tag))
    true
    false))

(defn replace-with [tag x]
  (println "replacing tag" tag x)
  (into [] (assoc x 0 tag)))

(defn fix-case-tags
  "resolve function-as symbol to function references in the reagent-hickup-map.
   Leaves regular hiccup data unchanged."
  [svg]
  (prewalk
   (fn [x]
     (cond (is-tag? :viewbox x) (replace-with :viewBox x)
           (is-tag? :textlength x) (replace-with :textLength x)
           (is-tag? :lengthAdjust x) (replace-with :lengthAdjust x)
           :else x))
   svg))

(defn inject-dimensions [w h hiccup-svg]
  (transform [1] #(assoc % :style {:width w :height h}) hiccup-svg))



(defn fix-svg [svg w h]
   (->> svg
        (ts/parse-string)
        (convert-style-as-strings-to-map)
        (fix-case-tags)
        (inject-dimensions w h)
        ))