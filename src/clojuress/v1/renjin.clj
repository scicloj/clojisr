(ns clojuress.v1.renjin
  (:require [alembic.still :refer [distill]]
            [clojuress.v1.session :as session]))

(distill '[org.renjin/renjin-script-engine "3.5-beta65"]
         :repositories {
                        "bedatadriven" {:url "https://nexus.bedatadriven.com/content/groups/public/"}
                        ;; Making sure https is used with Maven Central.
                        ;; See https://stackoverflow.com/a/59763928/1723677
                        "maven" {:url "https://repo.maven.apache.org/maven2" }})

(require '[clojuress.v1.impl.renjin.session :refer [make]])

(session/add-session-type!
 :renjin make)

(defn set-as-default! []
  (session/set-default-session-type! :renjin))

(defn set-as-default-if-missing! []
  (session/set-default-session-type-if-missing! :renjin))

(set-as-default-if-missing!)
