(defproject freecoin "0.2.0"
  :description "Freecoin digital currency toolkit"
  :url "http://freecoin.ch"
  :license {:name "GNU GPL Affero v3 and "
            :url "http://www.d-centproject.eu"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]
                 [liberator "0.14.1"]
                 [clj-http "3.1.0"]
                 [scenic "0.2.5"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-anti-forgery "1.0.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring.middleware.logger "0.5.0"]
                 [compojure "1.5.1"]
                 [org.slf4j/slf4j-simple "1.7.21"]
                 [org.clojars.d-cent/stonecutter-oauth "0.2.0-SNAPSHOT"]
                 [http-kit "2.1.19"]
                 [enlive "1.1.6"]
                 [formidable "0.1.10"]
                 [cheshire "5.6.3"]
                 [json-html "0.4.0"]
                 [autoclave "0.1.7" :exclusions [com.google.guava/guava com.google.code.findbugs/jsr305]]
                 [com.novemberain/monger "3.0.2"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.tiemens/secretshare "1.4.2"]
                 [buddy/buddy-hashers "0.14.0"]
                 [simple-time "0.2.1"]
                 [environ "1.0.3"]
                 [clojure-humanize "0.2.0"]
                 [clj.qrgen "0.4.0"]
                 [clavatar "0.3.0"]
                 [cc.artifice/lein-gossip "0.2.1"]
                 [circleci/clj-yaml "0.5.5"]]


  :source-paths ["src"]
  :resource-paths ["resources" "test-resources"]
  :jvm-opts ["-Djava.security.egd=file:/dev/random" ;use a proper random source (install haveged)
             "-XX:-OmitStackTraceInFastThrow" ; prevent JVM exceptions without stack trace
             ]
  :env [[:base-url "http://localhost:8000"]

        ;; trasnlation is configured here, strings are hard-coded at compile time
        ;; the last one acts as fallback if translated strings are not found
        [:translation-language "lang/it.yml"]
        [:translation-fallback "lang/en.yml"]]

  :aliases {"dev"  ["with-profile" "dev" "ring" "server"]
            "prod" ["with-profile" "production" "run"]
            "test-transactions" ["with-profile" "transaction-graph" "run"]}
  :profiles {:dev [:dev-common :dev-local]
             :dev-common {:dependencies [[midje "1.8.3"]
                                         [peridot "0.4.4"]
                                         [kerodon "0.8.0"]
                                         [ns-tracker "0.3.0"]]
                          :env [[:base-url "http://localhost:8000"]
                                [:client-id "LOCALFREECOIN"]
                                [:client-secret "FREECOINSECRET"]
                                [:auth-url "http://localhost:5000"]
                                [:secure "false"]]
                          :plugins [[lein-midje "3.1.3"]]}

             :rel [:release :release-local]
             :release {:env [[:base-url "http://demo.freecoin.ch:8000"]
                             [:client-id "dyne-demo-freecoin"]
                             [:client-secret "secret"]
                             [:auth-url "https://sso.dcentproject.eu"]
                             [:secure "true"]]}

             :transaction-graph [:dev :user
                                 {:dependencies [[org.clojure/test.check "0.9.0"]]
                                  :source-paths ["src" "scripts"]
                                  :main transaction-graph.main}]
             :production {:source-paths ["src" "prod"]
                          :main freecoin.main}
             :uberjar {:aot :all
                       :main freecoin.main}}
  :plugins [[lein-ring "0.9.3"]
            [lein-environ "1.0.0"]]
  :ring {:reload-paths ["src"]
         :init freecoin.core/lein-ring-init
         :handler freecoin.core/lein-ring-handler
         :destroy freecoin.core/lein-ring-stop
         :port 8000})
