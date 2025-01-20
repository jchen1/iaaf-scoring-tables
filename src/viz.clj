(ns viz
  (:require [clojure.data.json :as json]
            [oz.core :as oz]
            [oz.server :as oz-server]))


(def oz-port 10666)
(def graph-width 700)

(defn ensure-server-started!
  []
  (oz-server/start-server! oz-port))

(defn stop-server!
  []
  (oz-server/stop!))

(defn view!
  [plot & {:keys [host port mode :as config]}]
  (apply oz/view! plot config))

(defn export!
  [doc filepath & opts]
  (apply oz/export! doc filepath opts))

(defn all-data
  []
  (let [event "Road HM"
        gender "men"
        table (->> (json/read-str (slurp "iaaf-2025.json") :key-fn keyword)
                   (filter #(= {:event event :gender gender}
                               (select-keys % [:event :gender])))
                   (sort-by :mark))
        [a b c] (-> (json/read-str (slurp "coefficients-2025.json"))
                     (get-in [gender event]))
        coefficient-points (->> table
                                (map (fn [{:keys [mark] :as result}]
                                       (assoc result
                                         :coefficient-points (+ (* mark mark a) (* mark b) c)))))]
    coefficient-points))

(defn plot-difference
  []
  (let [data (all-data)]
    [:main
     [:h1 "Scoring tables vs regressions"]
     [:vega-lite {:title "Men's Marathon"
                  :data  {:values data}
                  :transform [{:fold [:points :coefficient-points]}]
                  :width graph-width
                  :mark     {:type   :point
                             :filled true}
                  :encoding {:x {:field :mark :type :quantitative :title "Mark (s)" #_#_  :scale {:domain [7200 21600]}}
                             :y {:field :value :type :quantitative :title "Points"}
                             :color {:field :key :type :nominal}}}]
     [:vega-lite {:title "Diff from tables (closer to 0 is better)"
                  :data  {:values (->> data
                                       (map #(assoc %
                                               :diff (- (:coefficient-points %) (:points %))
                                               :diff-floor (- (Math/floor (:coefficient-points %)) (:points %))
                                               :diff-ceil (- (Math/ceil (:coefficient-points %)) (:points %))
                                               :diff-round (- (Math/round ^double (:coefficient-points %)) (:points %)))))}
                  :transform [{:fold [:diff :diff-floor :diff-ceil :diff-round]}]
                  :width graph-width
                  :mark {:type :point
                         :filled true}
                  :encoding {:x {:field :mark :type :quantitative :title "Mark (s)" #_#_ :scale {:domain [7200 21600]}}
                             :y {:field :value :type :quantitative :title "Difference"}
                             :color {:field :key :type :nominal}}}]]))

(comment
  (-> (json/read-str (slurp "coefficients-2025.json"))
      (get-in ["men" "Road Marathon"]))
  (plot-difference)
  (take 1 (json/read-str (slurp "iaaf.json") :key-fn keyword))
  (ensure-server-started!)

  (view! (plot-difference))

  (stop-server!))