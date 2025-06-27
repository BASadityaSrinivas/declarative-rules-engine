(ns declarative-rules-engine.core
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
            [clojure.tools.logging :as log]))

(defn -main
  [& _]
  (println "Welcome to the Rule Engine !!"))

; ---------------------------- x ---------------------------- ;

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
    :gt (> ((first val) facts) (second val))
    :lt (< ((first val) facts) (second val))
    :eq (= ((first val) facts) (second val))
    :ne (not= ((first val) facts) (second val))
    :and (reduce #(and %1 (oper-eval %2 facts)) true (apply merge val))
    :or (reduce #(or %1 (oper-eval %2 facts)) false (apply merge val))
    false))

(defn rule-eval
  [{:keys [rule-id if then]} facts]
  (println rule-id if)
  (let [rule-pass? (reduce #(and %1 (oper-eval %2 facts))
                           true
                           if)]
    (log/info {:status "EVALUATED" :rule-id (name rule-id)})
    (when rule-pass?
      (log/info {:status "PASSED" :rule-id (name rule-id)})
      (actions then))
    {:rule-id rule-id
     :result rule-pass?
     :then (when rule-pass? :then/trigger-ventilation)}))

; ---------------------------- x ---------------------------- ;

(rule-eval {:rule-id :high-temperature
            :if {:or [{:and [{:gt [:sensor/temp 33]}
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

(s/def ::if (s/or :single (s/map-of ::operator ::sensor-spec :max-count 1 :min-count 1)
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
  `(if (contains? @rulebook ~rule-id)
     (log/warn {:status "DUPLICATE RULE" :rule-id (name ~rule-id)})
     (if (s/valid? ::rule-spec ~rule-def)
       (do (swap! rulebook assoc ~rule-id ~(assoc rule-def :rule-desc rule-desc))
           (log/info {:status "ADDED" :rule-id ~(name rule-id)}))
       (do (log/error {:status "FAILED"
                       :rule-id ~(name rule-id)
                       :rule-spec (s/explain-str ::rule-spec ~rule-def)})
           ;(throw (ex-info "INVALID RULE: Please read the rules to write a rule" {:rule (s/describe ::rule-spec)}))
           ))))

(defn defrule-fn
  [rule-id rule-desc rule-def]
  (if (contains? @rulebook rule-id)
    (log/warn {:status "DUPLICATE RULE" :rule-id (name rule-id)})
    (if (s/valid? ::rule-spec rule-def)
      (do (swap! rulebook assoc rule-id (assoc rule-def :rule-desc rule-desc))
          (log/info {:status "ADDED" :rule-id (name rule-id)}))
      (do (log/error {:status "FAILED"
                      :rule-id (name rule-id)
                      :rule-spec (s/explain-str ::rule-spec rule-def)})
          ;(throw (ex-info "INVALID RULE: Please read the rules to write a rule" {:rule (s/describe ::rule-spec)}))
          ))))

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

(defn gen-random-rule []
  (gen/fmap (fn [rule-id]
              {:rule-id rule-id
               :if {:gt [:sensor/temp (* 0.1 (rand-int 10000))]}
               :then :effect/trigger-ventilation})
            (gen/such-that #(> (count (name %)) 10) gen/keyword 100)))

(defn test-rule-validity []
  (let [random-rule (gen/sample (gen-random-rule) 10)]
    (doseq [rule random-rule]
      (println rule)
      (println (s/valid? ::rule-spec rule)))))

;(test-rule-validity)

;(gen/generate (s/gen (s/map-of ::operator ::sensor-spec :max-count 1 :min-count 1)))
