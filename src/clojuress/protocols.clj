(ns clojuress.protocols)

(defprotocol Session
  (close [session])
  (desc [session])
  (eval->jvm [session code])
  (jvm->set [session varname jvm-object])
  (get->jvm [session varname])
  (jvm->specified-type [session jvm-object type])
  (->clj [session jvm-object])
  (clj-> [session clj-object]))

(defprotocol Listlike
  (as-list [session]))
