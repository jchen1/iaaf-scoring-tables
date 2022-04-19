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
    (let [marks (merge (pdf/parse-pdf "pdfs/indoor.pdf" :indoor)
                       (pdf/parse-pdf "pdfs/outdoor.pdf" :outdoor))
          cols-to-insert (->> marks
                              (mapcat (fn [[k events]]
                                        (->> events
                                             (mapcat (fn [[event scores]]
                                                       (map (fn [[mark points]] [(namespace k) (name k) event mark points]) scores)))))))]
      (doseq [partition (partition-all 10 cols-to-insert)]
        (sql/insert-multi! conn :points db/all-columns (vec partition))))))

(defn marks-for-event
  [{:keys [gender category event] :as query}]
  (with-open [conn (db/conn)]
    (sql/find-by-keys conn :points
                      {:gender gender :category category :event event}
                      {:columns db/all-columns})))

(defn db->flat-json
  []
  (with-open [conn (db/conn)]
    (->> (sql/find-by-keys conn :points :all {:columns db/all-columns})
         (map (fn [m] (->> m (map (fn [[k v]] [(keyword (name k)) v])) (into {}))))
         (json/write-str)
         (spit (io/file "iaaf.json")))))

(defn write-coefficients
  []
  (with-open [conn (db/conn)]
    (let [coefficients (for [gender ["men" "women"]
                             category ["indoor" "outdoor"]
                             event constants/events]
                         (when-let [marks (some->> (marks-for-event {:gender gender :category category :event event})
                                                   not-empty
                                                   (map (fn [{:points/keys [mark points]}] [mark points])))]
                           (let [{:keys [coefficients]} (math/polynomial-regression (map first marks) (map second marks) 3)]
                             {:gender gender
                              :category category
                              :event event
                              :coefficients coefficients})))]
      (->> coefficients
           (keep identity)
           (util/nest-by [:category :gender :event])
           (walk/postwalk (fn [x]
                            (if (and (vector? x)
                                     (= 1 (count x))
                                     (map? (first x)))
                              (-> x first :coefficients)
                              x)))
           (json/write-str)
           (spit (io/file "coefficients.json"))))))

(defn -main
  [& args]
  (write-coefficients))

(comment
  (let [marks (->> (marks-for-event {:gender "men" :category "outdoor" :event "100m"})
                   (map (fn [{:points/keys [mark points]}] [mark points])))]
    (math/polynomial-regression (map first marks) (map second marks) 3))
  (let [marks (->> (marks-for-event {:gender "men" :category "outdoor" :event "100m"})
                   (map (fn [{:points/keys [mark points]}] [(/ 1 mark) points])))]
    (math/polynomial-regression (map first marks) (map second marks) 2))
  (-main))