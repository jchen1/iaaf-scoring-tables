(ns pdf
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripper]))

(def event->measure
  {"10 Miles"   :time
   "10,000m"    :time
   "10,000mW"   :time
   "1000m"      :time
   "100km"      :time
   "100m"       :time
   "100mH"      :time
   "10km"       :time
   "10kmW"      :time
   "110mH"      :time
   "15,000mW"   :time
   "1500m"      :time
   "15km"       :time
   "15kmW"      :time
   "2 Miles"    :time
   "20,000mW"   :time
   "2000m"      :time
   "2000mSC"    :time
   "200m"       :time
   "20km"       :time
   "20kmW"      :time
   "25km"       :time
   "30,000mW"   :time
   "3000m"      :time
   "3000mSC"    :time
   "3000mW"     :time
   "300m"       :time
   "30km"       :time
   "30kmW"      :time
   "35,000mW"   :time
   "35kmW"      :time
   "3kmW"       :time
   "400m"       :time
   "400mH"      :time
   "4x100m"     :time
   "4x200m"     :time
   "4x400m"     :time
   "50,000mW"   :time
   "5000m"      :time
   "5000mW"     :time
   "500m"       :time
   "50kmW"      :time
   "5km"        :time
   "5kmW"       :time
   "600m"       :time
   "800m"       :time
   "DT"         :distance
   "Heptathlon" :points
   "Decathlon"  :points
   "HJ"         :distance
   "HM"         :time
   "HT"         :distance
   "JT"         :distance
   "LJ"         :distance
   "Marathon"   :time
   "Mile"       :time
   "PV"         :distance
   "SP"         :distance
   "TJ"         :distance

   "50m" :time
   "55m" :time
   "60m" :time
   "50mH" :time
   "55mH" :time
   "60mH" :time
   "Pentathlon" :points})

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
                                                                                          measure (event->measure event)]
                                                                                      (when (and (not= event "Points") (nil? measure)) (prn event))
                                                                                      (when (and (not= result "-") (not= event "Points"))
                                                                                        [event [[(parse-mark (event->measure event) result) (Integer/parseInt points)]]]))))
                                                                    (into {}))))))]
                                 (update acc gender (fn [score-map]
                                                     (apply merge-with concat score-map page-points)))))))
                         {}
                         pages)]
      scores)))

(comment
  (-> (parse-pdf "pdfs/outdoor.pdf" :outdoor) keys)
  (parse-pdf "pdfs/indoor.pdf" :indoor))