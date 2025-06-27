(ns declarative-rules-engine.effects)

(defmulti actions
          "A multimethod that dispatches based on the type of effect to trigger the appropriate action.
           Each action corresponds to a specific effect, like triggering ventilation, alerting for rain, etc."
          (fn [x] x))

(defmethod actions :effect/trigger-ventilation
  [_]
  (println "Ventilation opened"))

(defmethod actions :effect/rain-alert
  [_]
  (println "Rain alert !!!"))

(defmethod actions :effect/theft-alert
  [_]
  (println "Burglar alert !!!"))

(defmethod actions :effect/fill-tank
  [_]
  (println "Water tank motor - ON"))

(defmethod actions :effect/tank-overflow
  [_]
  (println "Water overflow. Water tank motor - OFF"))
