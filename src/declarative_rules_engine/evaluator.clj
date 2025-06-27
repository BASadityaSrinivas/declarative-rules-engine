(ns declarative-rules-engine.evaluator
  (:require [clojure.tools.logging :as log]
            [declarative-rules-engine.effects :refer [actions]]))

(defn oper-eval
  "Evaluates a given condition (operator and value) against the provided facts.
   Supports operators such as :gt, :lt, :eq, :ne, :and, :or. It returns true or false based on the evaluation result.

   Parameters:
   - `op`: The operator to apply (e.g., :gt, :lt).
   - `val`: The value(s) for the operator to compare against the fact (e.g., [:sensor/temp 30]).
   - `facts`: A map of sensor data to evaluate the condition against.

   Returns:
   - A boolean indicating whether the condition is satisfied."
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
  "Evaluates a rule by checking its condition against the provided facts.
   If the condition is true, the corresponding action (effect) is triggered. Logs the evaluation status.

   Parameters:
   - `rule`: A map containing the rule definition, including `:rule-id`, `:if` (condition), and `:then` (action).
   - `facts`: A map of sensor data to evaluate the rule against.

   Returns:
   - A map containing the rule ID, the evaluation result (true or false), and the action to be triggered if the rule passes."
  [{:keys [rule-id if then]} facts]
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
