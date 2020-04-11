(ns clojisr.v1.robject
 (:require
     [pinkgorilla.ui.gorilla-renderable :refer [Renderable render]] )
  )

(defn- render-r-object [args]
  ((resolve 'clojisr.v1.gorilla-renderer/render-r-object) args))


;; Since IFn is an interface, not a protocol, we need to implement it here.
;; To do that, we need to resolve something from another namespace,
;; that we cannot depend upon (to avoid circular dependency).
;; See https://stackoverflow.com/a/3084773/1723677
;; After some long thought, couldn't find a more decent option,
;; except for uniting many, many namespaces into one huge namespace.
(defn- function [args]
  ((resolve 'clojisr.v1.functions/function) args))

;; See this related discussion on reifying IFn.
;; https://clojurians.zulipchat.com/#narrow/stream/215609-libpython-clj-dev/topic/pandas.20query.20methods.20bug/near/186410253
(defrecord RObject [object-name session code class]
  
  Renderable
  (render [self]
    (render-r-object self))

  clojure.lang.IFn
  (applyTo [this args]
    (apply (function this) args))
  (invoke [this] ((function this)))
  (invoke [this arg0]
    ((function this) arg0))
  (invoke [this arg0 arg1]
    ((function this) arg0 arg1))
  (invoke [this arg0 arg1 arg2]
    ((function this) arg0 arg1 arg2))
  (invoke [this arg0 arg1 arg2 arg3]
    ((function this)
     arg0
     arg1
     arg2
     arg3))
  (invoke [this arg0 arg1 arg2 arg3
           arg4]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6 arg7]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6 arg7 arg8]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6 arg7 arg8
           arg9]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6 arg7 arg8
           arg9 arg10]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10))
  (invoke [this arg0 arg1 arg2 arg3
           arg4 arg5 arg6 arg7 arg8
           arg9 arg10 arg11]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13 arg14]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13
     arg14))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13 arg14 arg15]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13
     arg14
     arg15))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13 arg14 arg15
     arg16]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13
     arg14
     arg15
     arg16))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13 arg14 arg15
     arg16 arg17]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13
     arg14
     arg15
     arg16
     arg17))
  (invoke
    [this arg0 arg1 arg2 arg3 arg4
     arg5 arg6 arg7 arg8 arg9 arg10
     arg11 arg12 arg13 arg14 arg15
     arg16 arg17 arg18]
    ((function this)
     arg0
     arg1
     arg2
     arg3
     arg4
     arg5
     arg6
     arg7
     arg8
     arg9
     arg10
     arg11
     arg12
     arg13
     arg14
     arg15
     arg16
     arg17
     arg18))
  #_(invoke
     [this arg0 arg1 arg2 arg3 arg4
      arg5 arg6 arg7 arg8 arg9 arg10
      arg11 arg12 arg13 arg14 arg15
      arg16 arg17 arg18 arg19]
     ((function this)
      arg0
      arg1
      arg2
      arg3
      arg4
      arg5
      arg6
      arg7
      arg8
      arg9
      arg10
      arg11
      arg12
      arg13
      arg14
      arg15
      arg16
      arg17
      arg18
      arg19))
  #_(invoke
     [this arg0 arg1 arg2 arg3 arg4
      arg5 arg6 arg7 arg8 arg9 arg10
      arg11 arg12 arg13 arg14 arg15
      arg16 arg17 arg18 arg19
      arg20-obj-array]
     (apply (function this)
            arg0
            arg1
            arg2
            arg3
            arg4
            arg5
            arg6
            arg7
            arg8
            arg9
            arg10
            arg11
            arg12
            arg13
            arg14
            arg15
            arg16
            arg17
            arg18
            arg19
            arg20-obj-array)))

(defn instance-robject? [o]
  (instance? RObject o))

(comment

  (instance-robject? [1 2 3])

  (instance-robject? (->RObject 1 2 3 4))


  ;; Generating this code
  (require '[zprint.core])
  (zprint.core/zprint
   (concat
    '(defrecord RObject [object-name session code]
       clojure.lang.IFn
       (applyTo [this args]
         (apply (function this) args)))
    (for [i   (range 21)
          :let [args (for [j (range i)]
                       (symbol (str "arg" j)))]]
      (list 'invoke
            (into ['this] args)
            (concat '((function this)) args)))
    [(let [args (concat (for [j (range 20)]
                          (symbol (str "arg" j)))
                        ['arg20-obj-array])]
       (list 'invoke
             (into ['this] args)
             (concat '(apply (function this)) args)))])
   40))

