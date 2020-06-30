(ns vrksasana.util
  (:require [clojure.string :as string]))

(defn rand-name []
  (-> (java.util.UUID/randomUUID)
      (string/replace "-" "")
      (->> (take 16)
           (cons \x)
           (apply str))))
