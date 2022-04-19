(ns math
  (:import [Jama Matrix QRDecomposition]))

(defn- find-vandermonde
  [xs degree]
  (loop [degree degree]
    (let [vandermonde
          (-> (for [i (range (count xs))]
                (->> (for [j (range degree)]
                      (Math/pow (nth xs i) j))
                    (into-array Double/TYPE)))
              into-array)
          matrix-x (Matrix. vandermonde)
          qr (QRDecomposition. matrix-x)]
      (if (.isFullRank qr)
        {:matrix-x matrix-x :qr qr :degree degree}
        (recur (dec degree))))))

(defn polynomial-regression
  "Adapted from https://algs4.cs.princeton.edu/14analysis/PolynomialRegression.java.html"
  [xs ys degree]
  (assert (= (count xs) (count ys)) "Must supply same number of xs as ys!")
  (let [{:keys [matrix-x qr degree]} (find-vandermonde xs degree)
        matrix-y (Matrix. ^doubles (into-array Double/TYPE ys) (count xs))
        beta (.solve qr matrix-y)
        mean-y (double (/ (apply + ys) (count ys)))
        sst (->> ys (map (fn [y] (Math/pow (- y mean-y) 2))) (apply +))
        residuals (.minus (.times matrix-x beta) matrix-y)
        sse (Math/pow (.norm2 residuals) 2)
        coefficients (->> (range degree)
                          (map (fn [j]
                                  (let [result (.get beta j 0)]
                                    (if (< (Math/abs result) 1E-4)
                                      0.0
                                      result))))
                          reverse
                          vec)]
    {:coefficients coefficients
     :r-squared (if (= sst 0) 1 (- 1 (/ sse sst)))}))

