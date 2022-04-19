(ns main
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [db]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [pdf]))

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

(defn -main
  [& args]
  (db->flat-json))

(comment
  (-main))