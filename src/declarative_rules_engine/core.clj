(ns declarative-rules-engine.core
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [declarative-rules-engine.facts :refer [facts]]
            [declarative-rules-engine.evaluator :refer [rule-eval]]
            [declarative-rules-engine.spec :as spec]))

; ---------------------------- x ---------------------------- ;

(def rulebook
  "Stores the collection of rules in the rulebook. Rules are added to the atom dynamically."
  (atom {}))

(defmacro defrule
  "A macro to define a new rule. It validates the rule against the rule specification, registers it in the rulebook,
   and logs the result of the registration. If the rule is invalid or duplicate, an error is logged.

   Parameters:
   - `rule-id`: The unique identifier for the rule.
   - `rule-desc`: A description of the rule's purpose or behavior.
   - `rule-def`: The rule definition containing the condition and action."
  [rule-id rule-desc rule-def]
  `(if (contains? @rulebook ~rule-id)
     (log/warn {:status "DUPLICATE RULE" :rule-id (name ~rule-id)})
     (if (s/valid? ::spec/rule-spec ~rule-def)
       (do (swap! rulebook assoc ~rule-id ~(assoc rule-def :rule-desc rule-desc))
           (log/info {:status "ADDED" :rule-id ~(name rule-id)}))
       (do (log/error {:status "FAILED"
                       :rule-id ~(name rule-id)
                       :rule-spec (s/explain-str ::spec/rule-spec ~rule-def)})
           (throw (ex-info "INVALID RULE: Please read the rules to write a rule" {:rule (s/describe ::spec/rule-spec)}))))))

(defn defrule-fn
  [rule-id rule-desc rule-def]
  (if (contains? @rulebook rule-id)
    (log/warn {:status "DUPLICATE RULE" :rule-id (name rule-id)})
    (if (s/valid? ::spec/rule-spec rule-def)
      (do (swap! rulebook assoc rule-id (assoc rule-def :rule-desc rule-desc))
          (log/info {:status "ADDED" :rule-id (name rule-id)}))
      (do (log/error {:status "FAILED"
                      :rule-id (name rule-id)
                      :rule-spec (s/explain-str ::spec/rule-spec rule-def)})
          ;(throw (ex-info "INVALID RULE: Please read the rules to write a rule" {:rule (s/describe ::spec/rule-spec)}))
          ))))

; ---------------------------- x ---------------------------- ;

; Rules Adder
(comment
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
           "Open the ventilation if the temperature is high and humidity is low"
           {:if {:or [{:and [{:gt [:sensor/temp 33]}
                             {:lt [:sensor/humidity 15]}]}
                      {:gt [:sensor/light-lux 10000]}]}
            :then :effect/trigger-ventilation})

  (defrule :water-tank-check-on
           "Turn on water tank motor if the water tank is low and a person is detected in the house"
           {:if {:and [{:lt [:sensor/water-tank 0.3]}
                       {:eq [:sensor/person-detected-in-house true]}]}
            :then :effect/fill-tank})

  (defrule :water-tank-full-check
           "Turn off water tank motor if the water tank is full"
           {:if {:gt [:sensor/water-tank 1.0]}
            :then :effect/tank-overflow})

  ; Duplicate case
  (defrule :water-tank-full-check
           "Turn off water tank motor if the water tank is full"
           {:if {:gt [:sensor/water-tank 0.9]}
            :then :effect/tank-overflow})

  ; Spec validation error
  (defrule :theft-check-999
           "Alert for any possible theft"
           {:if {:and [{:ne [:sensor/door-open? "false"]}
                       {:eq [:sensor/person-detected-in-house false]}]}
            :then :effect/theft-alert}))

; Rules Evaluator
(comment
  (mapv #(rule-eval (assoc (second %) :rule-id (first %)) facts) @rulebook))

