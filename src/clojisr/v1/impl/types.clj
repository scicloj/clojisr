(ns clojisr.v1.impl.types
  (:import [clojure.lang Named]
           [java.util Date]
           [java.time ZonedDateTime LocalDateTime LocalDate LocalTime OffsetTime OffsetDateTime ZoneId Instant]
           [java.time.format DateTimeFormatter]))

(defn primitive-r-type [clj-val]
  (cond (nil? clj-val) :na
        (integer? clj-val) :integer
        (number? clj-val) :numeric
        (string? clj-val) :character
        (keyword? clj-val) :factor
        (or (inst? clj-val)
            (instance? ZonedDateTime clj-val)
            (instance? LocalDateTime clj-val)) :time
        (instance? Boolean clj-val) :logical
        :else nil))

(def valid-coercions
  {:na        [:character :integer :numeric :factor :logical :time]
   :integer   [:integer :numeric :character]
   :numeric   [:numeric :character]
   :character [:character]
   :factor    [:factor :character]
   :logical   [:logical :character]
   :time      [:time :character]})

(def coercions-priorities
  (->> valid-coercions
       (mapcat val)
       frequencies))

(defn finest-primitive-r-type [clj-sequential]
  (let [n-elements (count clj-sequential)]
    (->> clj-sequential
         (mapcat (fn [elem]
                   (-> elem primitive-r-type valid-coercions)))
         frequencies
         (filter (fn [[_ n]]
                   (= n n-elements)))
         (map key)
         (sort-by coercions-priorities)
         first)))

;; to string

(defonce ^:private ^java.text.SimpleDateFormat date-format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss"))
(defonce ^:private ^DateTimeFormatter java-time-date-format (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))
(def ^:private java-time-formatters
  {ZonedDateTime java-time-date-format
   LocalDateTime java-time-date-format
   LocalDate DateTimeFormatter/ISO_LOCAL_DATE
   LocalTime DateTimeFormatter/ISO_LOCAL_TIME
   OffsetTime DateTimeFormatter/ISO_OFFSET_TIME
   OffsetDateTime java-time-date-format})

(defn ->to-zone
  [^ZonedDateTime dt]
  (str (.getZone dt)))

(defn ->str
  [x]
  (cond (nil? x) nil
        (inst? x) (.format date-format x)
        (contains? java-time-formatters (class x)) (let [^DateTimeFormatter fmt (java-time-formatters (class x))]
                                                     (.format fmt x))
        (instance? Named x) (name x)
        :else (str x)))

;; classes

(def strings-class (Class/forName "[Ljava.lang.String;"))
(defn ->strings
  #^"[Ljava.lang.String;" [xs]
  (if (= strings-class (class xs))
    xs
    (->> xs
         (map ->str)
         (into-array String))))

(def doubles-class (Class/forName "[D"))
(defn ->doubles
  #^"[D" [xs na]
  (if (= doubles-class (class xs))
    xs
    (->> xs
         (map (fn [x] (if (nil? x) na x)))
         (double-array))))

(def ints-class (Class/forName "[I"))
(defn ->integers
  #^"[I" [xs na]
  (if (= ints-class (class xs))
    xs
    (->> xs
         (map (fn [x] (if (nil? x) na x)))
         (int-array))))

(defn- datetime->double
  [dt]
  (cond
    (instance? Instant dt) (* 0.001 (.toEpochMilli ^Instant dt))
    (instance? Date dt) (datetime->double (.toInstant ^Date dt))
    (instance? ZonedDateTime dt) (datetime->double (.toInstant ^ZonedDateTime dt))
    (instance? LocalDateTime dt) (datetime->double (ZonedDateTime/of ^LocalDateTime dt (ZoneId/systemDefault)))
    :else dt))

(defn datetimes->doubles
  "Covnert time to doubles"
  #^"[D" [xs]
  (->> xs
       (map datetime->double)
       (double-array)))
