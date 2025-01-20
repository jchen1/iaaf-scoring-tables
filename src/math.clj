(ns math
  (:require [incanter.core :as incanter-core]
            [incanter.stats :as stats]))

(defn polynomial-regression
  [xs ys degree]
  (assert (= (count xs) (count ys)) "Must supply same number of xs as ys!")
  (let [X (reduce incanter-core/bind-columns (for [i (range 1 (inc degree))] (map #(Math/pow % i) xs)))]
    (stats/linear-model ys X)))

(defn least-squared
  "expects a list of [expected actual] tuples"
  [points]
  (->> points
       (map (fn [[x y]] (Math/pow (- x y) 2)))
       (apply +)))