(ns db
  (:require [next.jdbc :as jdbc]))

(def db
  {:dbtype "sqlite"
   :dbname "iaaf-2025.sqlite"})

(def create-table-tx
  ["CREATE TABLE IF NOT EXISTS points (
    id INTEGER PRIMARY KEY,
    gender TEXT NOT NULL,
    event TEXT NOT NULL,
    mark REAL NOT NULL,
    points INTEGER NOT NULL,
    UNIQUE(gender, event, mark),
    UNIQUE(gender, event, points)
  );"])

(def all-columns
  [:gender :event :mark :points])

(defn conn
  []
  (-> db jdbc/get-datasource jdbc/get-connection))