(ns clojisr.v1.util
  (:require [clojure.string :as string])
  (:import [java.io File]))

(defn first-if-one
  "Given a vector with a single element, first-if-one extracts that element; acts as identity otherwise.
  
       (first-if-one [:abc])
       => :abc
  
       (first-if-one [:abc :def])
       => [:abc :def]
  
       (first-if-one :abc)
       => :abc"
  {:added "0.1"}
  [form]
  (if (and (vector? form)
           (-> form count (= 1)))
    (first form)
    form))

(defn starts-with?
  "starts-with? checks if a given object s
  is sequential and its first element
  equals a given object v.
 
  (starts-with? 4 4)
  => false
 
  (starts-with? [4] 4)
  => true
 
  (starts-with? [4] 5)
  => false
 
  (starts-with? [] 5)
  => false
 
  (starts-with? [4 5] 4)
  => true
 
  (starts-with? [4 5] 5)
  => false
 
  (starts-with? '(4) 4)
  => true
 
  (starts-with? '(4) 5)
  => false
 
  (starts-with? '() 5)
  => false
 
  (starts-with? '(4 5) 4)
  => true
 
  (starts-with? '(4 5) 5)
  => false"
  {:added "0.1"} [s v]
  (and (sequential? s)
       (-> s first (= v))))

;; Thanks https://gist.github.com/sunng87/13700d3356d5514d35ad
(defn private-field [^Object obj field-name]
  (let [m (.. obj getClass (getDeclaredField field-name))]
    (. m (setAccessible true))
    (. m (get obj))))

;; Thanks https://blog.michielborkent.nl/2018/01/17/transducing-text/
(defn lines-reducible
  [^java.io.BufferedReader rdr]
  (reify clojure.lang.IReduceInit
    (reduce [_ f init]
      (try
        (loop [state init]
          (if (reduced? state)
            @state
            (if-let [line (.readLine rdr)]
              (recur (f state line))
              state)))
        (finally
          (.close rdr))))))

(defn -|>
  "A transformation analogous to the -> threading macro,
  but for vectors."
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form     (first forms)
            threaded (if (vector? form)
                       (into [(first form) x]
                             (next form))
                       [form x])]
        (recur threaded (next forms)))
      x)))



(comment
  (-|> 4
       :+
       [:* 10])
;; => [:* [:+ 4] 10]
  )


(defn tilde [s]
  (->> s
       name
       (str "~")
       keyword))

(defn file-exists? [^String path]
  (.exists (File. path)))

(defn rand-name []
  (-> (java.util.UUID/randomUUID)
      (string/replace "-" "")
      (->> (take 16)
           (cons \x)
           (apply str))))

(def l list)

(defn clojurize-r-symbol [s]
  (-> s
      name
      (string/replace #"\." "-")
      symbol))

(defn exception-cause [e]
  (-> e Throwable->map (select-keys [:via :cause])))

(def special-functions {"bra" "`[`"
                        "brabra" "`[[`"
                        "bra<-" "`[<-`"
                        "brabra<-" "`[[<-`"
                        "colon" "`:`"})


;; https://gist.github.com/apeckham/78da0a59076a4b91b1f5acf40a96de69
(defn get-free-port []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

;; symbol, string, how to process parameters (all or butlast)
(def bracket-data {"bra" ["`[`" true]
                   "brabra" ["`[[`" true]
                   "bra<-" ["`[<-`" false]
                   "brabra<-" ["`[[<-`" false]})

(defn maybe-wrap-backtick
  [string-or-symbol]
  (if (symbol? string-or-symbol)
    (name string-or-symbol)
    (str "`" (name string-or-symbol) "`")))
