(ns clojuress.v1.renjin
  (:require [clojuress.v1.session :as session]
            [alembic.still :refer [distill]]))

(distill '[org.renjin/renjin-script-engine "3.5-beta65"]
         :repositories {"bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}})

(require '[clojuress.v1.impl.renjin.session :refer [make]])

(session/add-session-type!
 :renjin make)

(defn set-as-default! []
  (session/set-default-session-type! :renjin))

(defn set-as-default-if-missing! []
  (session/set-default-session-type-if-missing! :renjin))
