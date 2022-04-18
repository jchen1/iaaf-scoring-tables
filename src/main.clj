(ns main
  (:require [pdf :as pdf]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]))

(def db
  {:dbtype "sqlite"
   :dbname "iaaf.sqlite"})

(def create-table-tx
  ["CREATE TABLE IF NOT EXISTS points (
    id INTEGER PRIMARY KEY,
    category TEXT NOT NULL,
    gender TEXT NOT NULL,
    event TEXT NOT NULL,
    mark REAL NOT NULL,
    points INTEGER NOT NULL,
    UNIQUE(category, gender, event, mark),
    UNIQUE(category, gender, event, points)
  );"])

(defn -main
  [& args]
  (with-open [conn (-> db jdbc/get-datasource jdbc/get-connection)]
    (jdbc/execute! conn create-table-tx)
    (let [marks (merge (pdf/parse-pdf "pdfs/indoor.pdf" :indoor)
                       (pdf/parse-pdf "pdfs/outdoor.pdf" :outdoor))
          cols-to-insert (->> marks
                              (mapcat (fn [[k events]]
                                        (->> events
                                             (mapcat (fn [[event scores]]
                                                       (map (fn [[mark points]] [(namespace k) (name k) event mark points]) scores)))))))]
      (doseq [partition (partition-all 10 cols-to-insert)]
        (sql/insert-multi! conn :points [:category :gender :event :mark :points] (vec partition))))))

(comment
  (jdbc/get-datasource db)
  (-main))