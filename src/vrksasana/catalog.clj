(ns vrksasana.catalog)

(def *state
  (atom {}))

(defn reset []
  (reset! *state
         {:ground-by-name              {}
          :season-by-name              {}
          :season-attributres-by-name  {}
          :tree-by-name                {}
          :default-ground-name         nil
          :ground->current-season-name {}
          :tree->seasons               {}
          :tree->current-season        {}
          :season->trees               {}}))

(defn ground-by-name [ground-name]
  (get-in @*state [:ground-by-name ground-name]))

(defn add-ground [ground-name ground]
  (swap! *state assoc-in
         [:ground-by-name ground-name] ground))

(defn default-ground []
  (-> @*state
      :default-ground-name
      ground-by-name))

(defn set-default-ground-name [ground-name]
  (swap! *state assoc :default-ground-name ground-name))

(defn current-season-name [ground]
  (-> @*state
      :ground->current-season-name
      (get ground)))

(defn define-season [season-name attributes]
  (swap! *state
         assoc-in
         [:season-attributres-by-name season-name]
         attributes))

(defn season-attributres-by-name [season-name]
  (get-in @*state [:season-attributres-by-name season-name]))

(defn add-season [season-name season]
  (swap! *state
         assoc-in
         [:season-by-name season-name]
         season))

(defn remove-season [season-name]
  (swap! *state
         update :season-by-name dissoc season-name))

(defn get-season [season-name]
  (get-in @*state [:season-by-name season-name]))

(defn set-current-season-name
  [ground season-name]
  (swap! *state
         assoc-in
         [:ground->current-season-name ground]
         season-name))

(defn active-season-names []
  (-> @*state :season-by-name keys))

(defn active-season? [season-name]
  (-> @*state :season-by-name (contains? season-name)))

(defn tree-fruit [tree]
  (-> @*state :tree->seasons (get tree)))

(defn add-tree [tree]
  (swap! *state
         assoc :tree-by-name
         (:tree-name tree) tree))

(defn tree-by-name [tree-name]
  (-> @*state :tree-by-name (get tree-name)))

(defn add-tree-when-missing [tree]
  (when-not
      (tree-by-name tree)
    (swap! *state assoc-in
           [:tree-by-name (:tree-name tree)] tree)))

(defn add-tree-to-season [season tree]
  (swap! *state
         (fn [s]
           (-> s
               (assoc-in [:tree->seasons tree]
                         season)
               (assoc-in [:season->trees season]
                         tree)))))
