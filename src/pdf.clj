(ns pdf
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [constants])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripper]))

(defmulti parse-mark (fn [mark-type _] mark-type))

(defmethod parse-mark :default
  [mark-type _]
  (throw (ex-info (format "Unknown mark type %s" mark-type) {})))

(defmethod parse-mark :time
  [_ time]
  (let [[seconds minutes hours] (map (fn [x] (Double/parseDouble x)) (reverse (str/split time #":")))]
    (+ (* 60 60 (or hours 0))
       (* 60 (or minutes 0))
       (or seconds 0))))

(defmethod parse-mark :distance
  [_ distance]
  (Double/parseDouble distance))

(defmethod parse-mark :points
  [_ points]
  (Integer/parseInt points))

(defn parse-header
  [header]
  (->> (str/split header #" +")
       (map str/trim)
       (reduce (fn [acc curr]
                 (if (and (= curr "Miles")
                          (contains? #{"2" "10"} (last acc)))
                   (conj (vec (drop-last acc)) (str (last acc) " Miles"))
                   (conj acc curr))) [])))

(defn parse-pdf
  [path prefix]
  (with-open
    [pdf (PDDocument/load (io/file path))]
    (let [stripper (doto (PDFTextStripper.)
                     (.setSortByPosition true))
          lines (keep not-empty (map str/trim (str/split-lines (.getText stripper pdf))))
          pages (->> (reduce (fn [{:keys [pages current-page] :as acc} line]
                               (if (try (do (Integer/parseInt line)
                                            true)
                                        (catch NumberFormatException _ false))
                                 {:pages        (conj pages current-page)
                                  :current-page []}
                                 (update acc :current-page conj line)))
                             {:pages [] :current-page []} lines)
                     :pages
                     (filter #(> (count %) 2))
                     (drop 1))
          scores (reduce (fn [acc [group header & scores]]
                           (let [gender (keyword (name prefix) (if (str/starts-with? group "MEN") "men" "women"))
                                 events (parse-header header)
                                 point-idx (.indexOf events "Points")]
                             (if (neg? point-idx)
                               acc
                               (let [page-points (->> scores
                                                      (map (fn [score]
                                                             (let [split (map str/trim (str/split score #" +"))
                                                                   points (nth split point-idx)]
                                                               (->> split
                                                                    (keep-indexed (fn [idx result]
                                                                                    (let [event (nth events idx)
                                                                                          measure (constants/event->measure event)]
                                                                                      (when (and (not= event "Points") (nil? measure)) (prn event))
                                                                                      (when (and (not= result "-") (not= event "Points"))
                                                                                        [event [[(parse-mark (constants/event->measure event) result) (Integer/parseInt points)]]]))))
                                                                    (into {}))))))]
                                 (update acc gender (fn [score-map]
                                                     (apply merge-with concat score-map page-points)))))))
                         {}
                         pages)]
      scores)))

(comment
  (-> (parse-pdf "pdfs/outdoor.pdf" :outdoor) keys)
  (parse-pdf "pdfs/indoor.pdf" :indoor))