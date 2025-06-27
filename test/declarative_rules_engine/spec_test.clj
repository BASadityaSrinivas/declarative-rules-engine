(ns declarative-rules-engine.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [declarative-rules-engine.spec :as spec]))

(defspec rules-spec-test
  10
  (prop/for-all [rule (s/gen ::spec/sensor-spec)]
    (println (s/explain ::spec/sensor-spec rule))))

(s/valid? ::spec/rule-spec
          {:rule-id :rain-check
           :if {:gt [:sensor/humidity 70]}
           :then :effect/rain-alert})

(s/valid? ::spec/rule-spec
          {:rule-id :rain-check
           :if {:and [{:gt [:sensor/humidity 70]}
                      {:eq [:sensor/person-detected-in-house false]}]}
           :then :effect/rain-alert})

(defn gen-random-rule []
  (gen/fmap (fn [rule-id]
              {:rule-id rule-id
               :if {:gt [:sensor/temp (rand-int 100)]}
               :then :effect/trigger-ventilation})
            (gen/such-that #(> (count (name %)) 10) gen/keyword 100)))

(defn test-rule-validity []
  (let [random-rule (gen/sample (gen-random-rule) 10)]
    (doseq [rule random-rule]
      (println rule)
      (println (s/valid? ::spec/rule-spec rule)))))

(comment
  (binding [s/*recursion-limit* 3]
    (gen/generate (s/gen ::spec/rule-spec)))
  (test-rule-validity)
  (gen/generate (s/gen ::spec/sensor-spec))
  (gen/generate (s/gen (s/map-of ::spec/operator ::spec/sensor-spec :max-count 1 :min-count 1))))
