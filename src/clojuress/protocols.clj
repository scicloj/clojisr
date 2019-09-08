(ns clojuress.protocols)

(defprotocol Session
  (close [session])
  (desc [session])
  (eval-r->java [session code])
  (java->r-set [session varname java-object])
  (get-r->java [session varname])
  (java->r-specified-type [session java-object type])
  (java->clj [session java-object])
  (clj->java [session clj-object]))

(defprotocol Listlike
  (as-list [session]))
