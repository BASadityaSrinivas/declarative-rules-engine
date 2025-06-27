(ns declarative-rules-engine.facts)

(def facts
  "A map of sensor data that represents the current facts for the rules engine to evaluate.
   The facts can include various sensor readings like temperature, humidity, and status of sensors."
  {:sensor/temp 35.2
   :sensor/humidity 87
   :sensor/soil-moisture 22.5
   :sensor/light-lux 12000
   :sensor/door-open? true
   :sensor/water-tank 1.2
   :sensor/person-detected-in-house false})
