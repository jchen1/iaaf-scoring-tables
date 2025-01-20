(ns constants)

(def event->measure
  {"10 Miles"    :time
   "10000m"      :time
   "10,000mW"    :time
   "1000m"       :time
   "1000m sh"    :time
   "100 km"      :time
   "100m"        :time
   "100mH"       :time
   "10 km"       :time
   "10kmW"       :time
   "110mH"       :time
   "15,000mW"    :time
   "1500m"       :time
   "1500m sh"    :time
   "15 km"       :time
   "15kmW"       :time
   "2 Miles"     :time
   "2 Miles sh"  :time
   "20,000mW"    :time
   "2000m"       :time
   "2000m sh"    :time
   "2000m SC"    :time
   "200m"        :time
   "200m sh"     :time
   "20 km"       :time
   "20kmW"       :time
   "25 km"       :time
   "30,000mW"    :time
   "3000m"       :time
   "3000m sh"    :time
   "3000m SC"    :time
   "3000mW"      :time
   "300m"        :time
   "300m sh"     :time
   "30 km"       :time
   "30kmW"       :time
   "35,000mW"    :time
   "35kmW"       :time
   "3kmW"        :time
   "400m"        :time
   "400m sh"     :time
   "400mH"       :time
   "4x100m"      :time
   "4x200m"      :time
   "4x200m sh"   :time
   "4x400m"      :time
   "4x400m sh"   :time
   "4x400mix"    :time
   "4x400mix sh" :time
   "50,000mW"    :time
   "5000m"       :time
   "5000m sh"    :time
   "5000mW"      :time
   "500m"        :time
   "500m sh"     :time
   "50kmW"       :time
   "5 km"        :time
   "5kmW"        :time
   "600m"        :time
   "600m sh"     :time
   "800m"        :time
   "800m sh"     :time
   "DT"          :distance
   "Hept."       :points
   "Hept. sh"    :points
   "Dec."        :points
   "HJ"          :distance
   "HM"          :time                                      ;; half-marathon
   "HT"          :distance
   "JT"          :distance
   "LJ"          :distance
   "Marathon"    :time
   "Mile"        :time
   "Mile sh"     :time
   "PV"          :distance
   "SP"          :distance
   "TJ"          :distance
   ;; indoor only
   "50m"         :time
   "55m"         :time
   "60m"         :time
   "50mH"        :time
   "55mH"        :time
   "60mH"        :time
   "Pentathlon"  :points
   "Pent. sh"    :points})

(def events (keys event->measure))
