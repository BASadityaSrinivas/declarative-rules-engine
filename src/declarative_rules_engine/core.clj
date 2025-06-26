(ns declarative-rules-engine.core
  (:require [clojure.spec.alpha :as s]))

(defn -main
  [& _]
  (println "Welcome to the Rule Engine !!"))

; ---------------------------- x ---------------------------- ;

(defn log
  [& msg]
  (println "log >> " (apply str msg)))

;; Input data
(def facts {:sensor/temp 35.2
            :sensor/humidity 87
            :sensor/soil-moisture 22.5
            :sensor/light-lux 12000
            :sensor/door-open? true
            :sensor/water-tank 1.2
            :sensor/person-detected-in-house false})

; ---------------------------- x ---------------------------- ;

(defmulti actions (fn [x] x))

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

; ---------------------------- x ---------------------------- ;

(defn oper-eval
  [[op val] facts]
  (case op
    :gt (> ((first val) data) (second val))
    :lt (< ((first val) data) (second val))
    :eq (= ((first val) data) (second val))
    :ne (not= ((first val) data) (second val))
    :and (reduce #(and %1 (oper-eval %2 facts)) true (apply merge val))
    :or (reduce #(or %1 (oper-eval %2 facts)) false (apply merge val))
    false))

(defn rule-eval
  [{:keys [rule-id if then]} facts]
  (println rule-id if)
  (let [rule-pass? (reduce #(and %1 (oper-eval %2 facts))
                           true
                           if)]
    (log (format "EVAL - %s" (name rule-id)))
    (when rule-pass?
      (actions then))
    {:rule-id rule-id
     :result rule-pass?
     :then (when rule-pass? :then/trigger-ventilation)}))

; ---------------------------- x ---------------------------- ;

(rule-eval {:rule-id :high-temperature
            :if {:or [{:and [{:gt [:sensor/temp 37]}
                             {:lt [:sensor/humidity 15]}]}
                      {:gt [:sensor/light-lux 10000]}]}
            :then :effect/trigger-ventilation}
           facts)

(rule-eval {:rule-id :high-temperature
            :if {:gt [:sensor/temp 37]}
            :then :effect/trigger-ventilation}
           facts)

(rule-eval {:rule-id :water-tank-check-1
            :if {:and [{:lt [:sensor/water-tank 0.3]}
                       {:eq [:sensor/person-detected-in-house true]}]}
            :then :effect/fill-tank}
           facts)

(rule-eval {:rule-id :water-tank-check-2
            :if {:gt [:sensor/water-tank 1.0]}
            :then :effect/tank-overflow}
           facts)

(rule-eval {:rule-id :theft-check
            :if {:and [{:ne [:sensor/door-open? false]}
                       {:eq [:sensor/person-detected-in-house false]}]}
            :then :effect/theft-alert}
           facts)

(rule-eval {:rule-id :rain-check
            :if {:and [{:gt [:sensor/humidity 70]}
                       {:eq [:sensor/person-detected-in-house false]}]}
            :then :effect/rain-alert}
           facts)

; ---------------------------- x ---------------------------- ;

(s/def ::rule-id keyword?)

(s/def ::then #{:effect/trigger-ventilation
                :effect/rain-alert
                :effect/theft-alert
                :effect/fill-tank
                :effect/tank-overflow})

(s/def ::cond #{:and :or})
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

(s/def ::if (s/or :single (s/map-of ::operator ::sensor-spec)
                  :multiple (s/map-of ::cond (s/coll-of ::if))))

(s/def ::rule-spec (s/keys :req-un [::if ::then]))

; ---------------------------- x ---------------------------- ;

(s/valid? ::rule-spec
          {:rule-id :rain-check
           :if {:gt [:sensor/humidity 70]}
           :then :effect/rain-alert})

(s/valid? ::rule-spec
          {:rule-id :rain-check
           :if {:and [{:gt [:sensor/humidity 70]}
                      {:eq [:sensor/person-detected-in-house false]}]}
           :then :effect/rain-alert})

; ---------------------------- x ---------------------------- ;

(def rulebook (atom {}))

(defmacro defrule
  [rule-id rule-desc rule-def]
  ;validate the rule against spec
  ;register it into the rulebook or equivalent
  ;optionally log registration for observability
  `(if-not (s/valid? ::rule-spec ~rule-def)
     (do (log "RULE-ADD FAILED:" ~(name rule-id) " | " (s/explain ::rule-spec ~rule-def))
         ;(throw (ex-info "INVALID RULE: Please read the rules to write a rule" {:rule (s/describe ::rule-spec)}))
         )
     (do (swap! rulebook assoc ~rule-id ~(assoc rule-def :rule-desc rule-desc))
         (log "RULE-ADD:" ~(name rule-id)))))

(defrule :rain-check
         "Alert if it is going to rain soon"
         {:if {:and [{:gt [:sensor/humidity 70]}
                     {:eq [:sensor/person-detected-in-house false]}]}
          :then :effect/rain-alert})

(defrule :theft-check
         "Alert for any possible theft"
         {:if {:and [{:ne [:sensor/door-open? false]}
                     {:eq [:sensor/person-detected-in-house false]}]}
          :then :effect/theft-alert})

(defrule :high-temperature
         "Open the ventilation if the temperature is high"
         {:if {:gt [:sensor/temp 37]}
          :then :effect/trigger-ventilation})

(defrule :theft-check
         "Alert for any possible theft"
         {:if {:and [{:ne [:sensor/door-open? "false"]}
                     {:eq [:sensor/person-detected-in-house false]}]}
          :then :effect/theft-alert})

(mapv #(rule-eval (assoc (second %) :rule-id (first %)) facts) @rulebook)

; ---------------------------- x ---------------------------- ;

