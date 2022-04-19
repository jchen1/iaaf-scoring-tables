(ns util)

(defn nest-by
  [ks coll]
  (let [keyfn (apply juxt ks)]
    (reduce (fn [m x] (update-in m (keyfn x) (fnil conj []) x)) {} coll)))
