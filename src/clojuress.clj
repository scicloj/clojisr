(ns clojuress
  (:require [clojuress.session :as session]
            [clojuress.rlang :as rlang]
            [clojuress.protocols :as prot]
            [clojure.pprint :as pp])
  (:import clojuress.robject.RObject))


(defmacro defn-implicit-session
  "This is just some syntactic sugar to define functions
 that may optionally get session as an argument,
 and otherwise use the default session. 
 
  (defn-implicit-session r [r-code]
    (rlang/eval-r r-code session))
  =expands-to=>
  (defn r [r-code & {:keys [session-args]}]
    (let [session (session/fetch-or-make session-args)]
     (rlang/eval-r r-code session)))"
  {:added "0.1"}
  [f args & body]
  (concat (list 'defn
                f
                (into args '[& {:keys [session-args]}]))
          (list (concat (list 'let
                              '[session (session/fetch-or-make session-args)]
                              )
                        body))))

(defn init-session
  "TODO"
  {:added "0.1"} [& {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (rlang/init-session session)))

(defn r
  "r runs r code and returns a (handle of) the r return value.
 Like most of the API functions, a session can be specified explicitly.
 
       (-> \"1+2\"
            r
            r->java->clj)
       => [3.0]
 
       (-> \"1+2\"
           (r :session-args {:port 4444})
            r->java->clj)
       => [3.0]"
  {:added "0.1"} [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (rlang/eval-r r-code session)))



(defn eval-r->java
  "eval-r->java runs r code and returns the corresponding java object
 (precise definition depends on session type).
 
       (-> \"1+2\"
           eval-r->java
           class)
       => REXPDouble
 
       (-> \"1+2\"
           eval-r->java
           (.asDoubles)
           vec)
       => [3.0]"
  {:added "0.1"} [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn eval-r->java
  "eval-r->java runs r code and returns the corresponding java object
 (precise definition depends on session type).
 
       (-> \"1+2\"
           eval-r->java
           class)
       => REXPDouble
 
       (-> \"1+2\"
           eval-r->java
           (.asDoubles)
           vec)
       => [3.0]"
  {:added "0.1"} [r-code & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/eval-r->java session r-code)))

(defn r-class
  "r-class gets the class of an R object.
 
       (-> \"1+2\"
           r
           r-class)
       => [\"numeric\"]"
  {:added "0.1"}
  [r-object]
  (rlang/r-class r-object))

(defn names
  "names gets the names attribute of an R object.
 
       (-> \"data.frame(x=1:3,y='hi')\"
           r
           names)
       => [\"x\" \"y\"]"
  {:added "0.1"}
  [r-object]
  (rlang/names r-object))

(defn shape
  "shape gets the dim (dimension) attribute of an R object.
 
       (-> \"matrix(1:6,2,3)\"
           r
           shape)
       => [2 3]"
  {:added "0.1"}
  [r-object]
  (rlang/shape r-object))

(defn r->java
  "r->java converts an R object to a java object.
 The precise definition depends on the session implementation.
 For Rserve sessions, this will be something that inherits from
 org.rosuda.REngine.REXP.
 
       (->> \"1:9\"
            r
            r->java
            class)
       => REXPInteger"
  {:added "0.1"}
  [r-object]
  (rlang/r->java r-object))

(defn java->r
  "java->r converts a java object to an r object.
 (precise definition depends on session type).
 
       (-> (REXPInteger. 1)
           java->r
           r->java->clj)
       => [1]"
  {:added "0.1"} [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (rlang/java->r java-object session)))

(defn java->naive-clj
  "java->naive-clj converts a java object of the underlying
 java r-interop engine (precise definition depending on session)
 into clojure, but in a naive way (e.g., not taking care of missing values)."
  {:added "0.1"} [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->naive-clj session java-object)))

(defn java->clj
  "java->naive-clj converts a java object of the underlying
 java r-interop engine (precise definition depending on session)
 into clojure.
 
       (-> \"list(a=1:2,b='hi!')\"
           r
           r->java
           java->clj)
       => {:a [1 2] :b [\"hi!\"]}"
  {:added "0.1"} [java-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/java->clj session java-object)))

(defn clj->java
  "clj->java converts a clojure data structure
 to a java object of the underlying java r-interop engine
 (precise definition depending on session).
 
       (-> {:a [1 2] :b \"hi!\"}
           clj->java
           java->r
           r->java->clj)
       => {:a [1 2] :b [\"hi!\"]}
 
       (-> {:a [1 2] :b \"hi!\"}
           clj->java
           java->r
           base/deparse
           r->java->clj)
       => [\"list(a = 1:2, b = \\\"hi!\\\")\"]"
  {:added "0.1"} [clj-object & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (prot/clj->java session clj-object)))

(def clj->java->r (comp java->r clj->java))

(def r->java->clj (comp java->clj r->java))


(defn discard-session [session-args]
  (session/discard session-args))

(defn discard-default-session []
  (session/discard-default))

(defn fresh-object? [r-object]
  (-> r-object
      :session
      session/fresh?))

(defn refreshed-object [r-object]
  (if (fresh-object? r-object)
    ;; The object is refresh -- just return it.
    r-object
    ;; Try to return a refreshed object.
    (if-let [code (:code r-object)]
      ;; The object has code information -- rerun the code with the same session-args.
      (r code
         :session-args (:session-args r-object))
      ;; No code information.
      (ex-info "Cannot refresh an object with no code info." {:r-object r-object}))))

(defn auto-refresing-object [r-object]
  (let [mem (atom r-object)]
    (reify clojure.lang.IDeref
      (deref [_]
        (when (-> @mem fresh-object? not)
          (reset! mem (refreshed-object r-object)))
        @mem))))


(defn apply-function
  "apply-function applies an R function to given arguments.
 
 It expects a data structure
 containing the arguments,
 with possibly named arguments.
 
 If necessary, arguments are converted to R. 
 
       (let [r-function (-> \"function(w,x,y=10,z=20) w+x+y+z\"
                            r)]
         (->> [[1 2]
               [1 2 [:= :y 100]]
               [1 2 [:= :z 100]]]
              (map (fn [args]
                     (->> args
                          (apply-function r-function)
                          r->java->clj)))))
       => [[33.0] [123.0] [113.0]]"
  {:added "0.1"} [r-function args & {:keys [session-args]}]
  (let [session (session/fetch-or-make session-args)]
    (rlang/apply-function r-function args session)))

(defn function
  "function creates a Clojure function
 that wraps a given R function
 and acts on R objects.
 
 Named arguments are supported.
 
 If necessary, arguments are converted to R.
 
       (let [f (->> \"function(w,x,y=10,z=20) w+x+y+z\"
                    r
                    function)]
         (->> [(f 1 2)
               (f 1 2 [:= :y 100])
               (f 1 2 [:= :z 100])]
              (map r->java->clj)))
       => [[33.0] [123.0] [113.0]]"
  {:added "0.1"}
  [r-function]
  (let [autorefreshing (auto-refresing-object
                        r-function)]
    (fn f
      ([& args]
       (let [explicit-session-args
             (when (some-> args butlast last (= :session-args))
               (last args))]
         (apply-function
          @autorefreshing
          (if explicit-session-args
            (-> args butlast butlast)
            args)
          :session (session/fetch-or-make explicit-session-args)))))))

(defn add-functions-to-this-ns
  "add-functions-to-this-ns adds to the current namespace
 a symbol bound to a clojure function wrapping a given r function."
  {:added "0.1"}
  [package-symbol function-symbols]
  (doseq [s function-symbols]
    (let [d (delay (function
                    (r (format "{library(%s); %s}"
                               (name package-symbol)
                               (name s)))))
          f (fn [& args]
              (apply @d args))]
      (eval (list 'def s f)))))


(defn add-package-to-this-ns
  [package-symbol]
  (->> package-symbol
       name
       (format "library(%s)")
       r)
  (->> package-symbol
       name
       vector
       (apply-function (r "function(package_name) as.character(unlist(lsf.str(paste0('package:', package_name))))"))
       r->java->clj
       (filter (fn [function-name]
                 (re-matches #"[A-Za-z][A-Za-z\\.\\_].*" function-name)))
       (map symbol)
       (add-functions-to-this-ns package-symbol)))

;; Overriding pprint
(defmethod pp/simple-dispatch RObject [obj]
  (let [java-object (r->java obj)]
    (pp/pprint [['R
                 :object-name (:object-name obj)
                 :session-args (-> obj :session :session-args)
                 :r-class (r-class obj)]
                ['->Java java-object]])))


