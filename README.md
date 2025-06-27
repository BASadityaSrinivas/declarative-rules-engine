# Declarative Rules Engine
A lightweight, idiomatic rules engine built in Clojure that allows users to define declarative validation 
and control rules. The engine evaluates dynamic input data (facts) against these rules and triggers actions (effects) 
accordingly. This project uses Clojure.spec for validation, multimethods for action dispatching, and provides 
a flexible, extensible architecture.

## Project structure
```
declarative-rules-engine/
├── LICENSE
├── project.clj
├── README.md
├── resources/
├── src/
│   └── declarative_rules_engine/
│       ├── core.clj                    # Main logic and entry point
│       ├── effects.clj                 # Action dispatching (effect handlers)
│       ├── evaluator.clj               # Rule evaluation logic
│       ├── facts.clj                   # Facts and fact validation
│       └── spec.clj                    # Clojure.spec for rule and fact validation
├── test/
│   └── declarative_rules_engine/
│       └── spec_test.clj               # Tests for spec validation, rule evaluation
```

### Example of Rule Definition

```clojure
(defrule :high-temperature
         "Open the ventilation if the temperature is high and humidity is low"
         {:if {:or [{:and [{:gt [:sensor/temp 33]}
                            {:lt [:sensor/humidity 15]}]}
                   {:gt [:sensor/light-lux 10000]}]}
          :then :effect/trigger-ventilation})
```

- The rule is named `:high-temperature`.
- It triggers the `:effect/trigger-ventilation` action if:
  - The temperature is above 33°C and humidity is below 15%, or
  - The light intensity is greater than 10,000 lux.

### Example of Rule Evaluation

```clojure
(rule-eval {:rule-id :high-temperature
            :if {:gt [:sensor/temp 37]}
            :then :effect/trigger-ventilation}
           facts)
```

### Adding New Rules
You can add new rules by defining them using the defrule macro. Each rule should have:

- A unique `:rule-id`.
- A condition (`:if`) that uses operators like `:gt`, `:lt`, `:eq`, and logical combinations like `:and` or `:or`.
- An action (`:then`) that corresponds to an effect, like `:effect/trigger-ventilation`.

```clojure
(defrule :rain-check
         "Alert if it is going to rain soon"
         {:if {:and [{:gt [:sensor/humidity 70]}
                     {:eq [:sensor/person-detected-in-house false]}]}
          :then :effect/rain-alert})
```