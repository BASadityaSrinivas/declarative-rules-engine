(ns declarative-rules-engine.spec-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]))

(def rule-spec :declarative-rules-engine.spec/rule-spec)

(s/valid? rule-spec
          {:rule-id :rain-check
           :if {:gt [:sensor/humidity 70]}
           :then :effect/rain-alert})

(s/valid? rule-spec
          {:rule-id :rain-check
           :if {:and [{:gt [:sensor/humidity 70]}
                      {:eq [:sensor/person-detected-in-house false]}]}
           :then :effect/rain-alert})