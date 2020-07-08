(ns clojisr.v1.protocols)

(defprotocol Session
  (close [session])
  (closed? [session])
  (id [session])
  (session-args [session])
  (desc [session])
  (eval-r->java [session code])
  (java->r-set [session varname java-object])
  (print-to-string [session r-obj])
  (package-symbol->r-symbol-names [session package-symbol]))
