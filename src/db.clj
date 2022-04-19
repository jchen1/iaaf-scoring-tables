(ns db
  (:require [next.jdbc :as jdbc]))

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

(def all-columns
  [:category :gender :event :mark :points])

(defn conn
  []
  (-> db jdbc/get-datasource jdbc/get-connection))