(defproject declarative-rules-engine "0.1.0-SNAPSHOT"
  :description "A lightweight, idiomatic rules engine built in Clojure."
  :aliases {"magik" ["run"]}
  :dependencies [[org.clojure/clojure "1.12.1"]
                 [org.clojure/test.check "1.1.1"]
                 [org.clojure/tools.logging "1.3.0"]]
  :main ^:skip-aot declarative-rules-engine.core
  :target-path "target/%s"
  :test-paths ["test"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
