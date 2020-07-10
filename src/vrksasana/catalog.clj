(ns vrksasana.catalog)

(def *state
  (atom {}))

(defn reset []
  (reset! *state
          {:ground-name->ground             {}
           :season-name->season             {}
           :season-name->season-attributres {}
           :tree-name->tree                 {}
           :default-ground-name             nil
           :ground->current-season-name     {}
           :tree->seasons                   {}
           :tree->current-season            {}
           :season->trees                   {}}))

(defn ground-name->ground [ground-name]
  (get-in @*state [:ground-name->ground ground-name]))

(defn add-ground [ground-name ground]
  (swap! *state assoc-in
         [:ground-name->ground ground-name] ground))

(defn default-ground []
  (-> @*state
      :default-ground-name
      ground-name->ground))

(defn set-default-ground-name [ground-name]
  (swap! *state assoc :default-ground-name ground-name))

(defn current-season-name [ground]
  (-> @*state
      :ground->current-season-name
      (get ground)))

(defn seasons []
  (->> @*state
       :season-name->season
       vals))

(defn define-season [season-name attributes]
  (swap! *state
         assoc-in
         [:season-name->season-attributres season-name]
         attributes))

(defn season-name->season-attributres [season-name]
  (get-in @*state [:season-name->season-attributres season-name]))

(defn add-season [season-name season]
  (swap! *state
         assoc-in
         [:season-name->season season-name]
         season))

(defn remove-season [season-name]
  (swap! *state
         update :season-name->season dissoc season-name))

(defn get-season [season-name]
  (get-in @*state [:season-name->season season-name]))

(defn set-current-season-name
  [ground season-name]
  (swap! *state
         assoc-in
         [:ground->current-season-name ground]
         season-name))

(defn active-season-names []
  (-> @*state :season-name->season keys))

(defn active-season? [season-name]
  (-> @*state :season-name->season (contains? season-name)))

(defn tree-fruit [tree]
  (-> @*state :tree->seasons (get tree)))

(defn add-tree [tree]
  (swap! *state
         assoc :tree-name->tree
         (:tree-name tree) tree))

(defn tree-name->tree [tree-name]
  (-> @*state :tree-name->tree (get tree-name)))

(defn add-tree-when-missing [tree]
  (when-not
      (tree-name->tree tree)
    (swap! *state assoc-in
           [:tree-name->tree (:tree-name tree)] tree)))

(defn add-tree-to-season [season tree]
  (swap! *state
         (fn [s]
           (-> s
               (assoc-in [:tree->seasons tree]
                         season)
               (assoc-in [:season->trees season]
                         tree)))))
