(ns declarative-rules-engine.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::rule-id keyword?)

(s/def ::then #{:effect/trigger-ventilation
                :effect/rain-alert
                :effect/theft-alert
                :effect/fill-tank
                :effect/tank-overflow})

(s/def ::condition #{:and :or})
(s/def ::operator #{:gt :lt :eq :ne})

(s/def ::value (s/or :bool boolean?
                     :int (s/and int?
                                 #(>= % 0)
                                 #(< % 20000))
                     :float (s/and float?
                                   #(>= % 0)
                                   #(<= % 100))))

(s/def ::sensor-spec (s/cat :sensor #{:sensor/temp
                                      :sensor/humidity
                                      :sensor/soil-moisture
                                      :sensor/light-lux
                                      :sensor/door-open?
                                      :sensor/water-tank
                                      :sensor/person-detected-in-house}
                            :value ::value))

(s/def ::if (s/or :single (s/map-of ::operator ::sensor-spec :max-count 1 :min-count 1)
                  :multiple (s/map-of ::condition (s/coll-of ::if))))

(s/def ::rule-spec (s/keys :req-un [::if ::then]))
