(ns vrksasana.impl.r.season
  (:require [vrksasana.season]
            [clojisr.v1.protocols]
            [clojisr.v1.session :as session]
            [clojisr.v1.using-sessions :as using-sessions]
            [clojisr.v1.impl.java-to-clj :as java2clj]
            [clojisr.v1.impl.clj-to-java :as clj2java]))

(deftype Season [ground season-name attributes session]
  vrksasana.season/PSeason
  (eval-code [this code]
    (using-sessions/eval-code code session))
  (string-to-print [this fruit]
    (->> fruit
         :value
         (clojisr.v1.protocols/print-to-string session)))
  (ground [this] ground)
  (season-name [this] season-name)
  (attributes [this] attributes)
  (close [this]
    (clojisr.v1.protocols/close session))
  (fruit-value->data [this fruit-value]
    (-> fruit-value
        using-sessions/r->java
        java2clj/java->clj))
  (data->fruit-value [this data]
    (-> (clj2java/clj->java session data)
        (using-sessions/java->r session))))

(defn new-season [ground season-name attributes]
  (let [*session (delay
                   (session/make attributes))]
    (clojisr.v1.session/init @*session)
    (->Season ground season-name attributes @*session)))
