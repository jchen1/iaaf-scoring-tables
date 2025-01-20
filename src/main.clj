(ns main
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [db]
            [math]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pdf]
            [util]
            [clojure.walk :as walk]))

(defn create-db
  []
  (with-open [conn (db/conn)]
    (jdbc/execute! conn db/create-table-tx)
    (let [marks (pdf/parse-pdf "pdfs/combined-2025.pdf")
          cols-to-insert (->> marks
                              (mapcat (fn [[k events]]
                                        (->> events
                                             (mapcat (fn [[event scores]]
                                                       (map (fn [[mark points]] [(name k) event mark points]) scores)))))))]
      (doseq [partition (partition-all 1000 cols-to-insert)]
        (try (sql/insert-multi! conn :points db/all-columns (vec partition))
             (catch Exception ex
               (println partition)))))))

(defn marks-for-event
  [{:keys [gender event] :as query}]
  (with-open [conn (db/conn)]
    (sql/find-by-keys conn :points
                      {:gender gender :event event}
                      {:columns db/all-columns})))

(defn all-events-in-db
  []
  (with-open [conn (db/conn)]
    (->> (sql/query conn ["select distinct event from points"])
         (map :points/event))))

(defn db->flat-json
  []
  (with-open [conn (db/conn)]
    (->> (sql/find-by-keys conn :points :all {:columns db/all-columns})
         (map (fn [m] (->> m (map (fn [[k v]] [(keyword (name k)) v])) (into {}))))
         (json/write-str)
         (spit (io/file "iaaf-2025.json")))))

(defn write-coefficients
  []
  (let [coefficients (for [gender ["men" "women"]
                           event (all-events-in-db)]
                       (when-let [marks (some->> (marks-for-event {:gender gender :event event})
                                                 not-empty
                                                 (map (fn [{:points/keys [mark points]}] [mark points])))]
                         (let [{:keys [coefs r-square f-stat f-prob]} (math/polynomial-regression (map first marks) (map second marks) 2)]
                           {:gender       gender
                            :event        event
                            :coefficients (reverse coefs)})))]
    (->> coefficients
         (keep identity)
         (util/nest-by [:gender :event])
         (walk/postwalk (fn [x]
                          (if (and (vector? x)
                                   (= 1 (count x))
                                   (map? (first x)))
                            (-> x first :coefficients)
                            x)))
         (json/write-str)
         (spit (io/file "coefficients-2025.json")))))

(defn -main
  [& args]
  (let [category "outdoor"
        event "100m"
        gender "men"
        table (->> (json/read-str (slurp "iaaf-2025.json") :key-fn keyword)
                   (filter #(= {:category category :event event :gender gender}
                               (select-keys % [:category :event :gender])))
                   (sort-by :mark))
        [a b c] (->> (json/read-str (slurp "coefficients-2025.json") :key-fn keyword)
                     :outdoor
                     :men
                     :100m)
        coefficient-points (->> table
                                (map (fn [{:keys [mark] :as result}]
                                       (assoc result
                                         :coefficient-points (+ (* mark mark a) (* mark b) c)))))]
    {:diff       (math/least-squared (->> coefficient-points
                                          (map (fn [{:keys [points coefficient-points]}]
                                                 [points coefficient-points]))))
     :diff-floor (math/least-squared (->> coefficient-points
                                          (map (fn [{:keys [points coefficient-points]}]
                                                 [points (Math/floor coefficient-points)]))))
     :diff-ceil  (math/least-squared (->> coefficient-points
                                          (map (fn [{:keys [points coefficient-points]}]
                                                 [points (Math/ceil coefficient-points)]))))
     :diff-round (math/least-squared (->> coefficient-points
                                          (map (fn [{:keys [points coefficient-points]}]
                                                 [points (Math/round ^double coefficient-points)]))))}))

(comment
  (all-events-in-db)
  (write-coefficients)
  (do
    (io/delete-file "iaaf-2025.sqlite")
    (create-db)
    (write-coefficients)
    (db->flat-json))

  (let [marks (->> (main/marks-for-event {:gender "men"  :event "HT"})
                   (map (fn [{:points/keys [mark points]}] [mark points])))]
    (math/polynomial-regression (map first marks) (map second marks) 2))
  (let [marks (->> (marks-for-event {:gender "men"  :event "100m"})
                   (map (fn [{:points/keys [mark points]}] [(/ 1 mark) points])))]
    (math/polynomial-regression (map first marks) (map second marks) 2))
  (-main))