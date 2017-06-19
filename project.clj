(def ns-tracker-version "0.3.1")

(defproject freecoin "0.2.0"
  :description "Freecoin digital currency toolkit"
  :url "http://freecoin.ch"
  :license {:name "GNU GPL Affero v3 and "
            :url "http://www.d-centproject.eu"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.8.0"]
                 [liberator "0.14.1" :exclusions [hiccup]]
                 [clj-http "3.4.1"]
                 [scenic "0.2.5"]
                 [ring/ring-core "1.5.1"]
                 [ring/ring-anti-forgery "1.0.1"]
                 [ring/ring-defaults "0.2.3"]
                 [ring.middleware.logger "0.5.0" :exclusions [org.slf4j/slf4j-api]]
                 [compojure "1.5.2"] 
                 [http-kit "2.2.0"]
                 [enlive "1.1.6"]
                 [formidable "0.1.10"]
                 [cheshire "5.7.0"]
                 [json-html "0.4.0"]
                 [autoclave "0.1.7" :exclusions [com.google.guava/guava com.google.code.findbugs/jsr305]]
                 [com.novemberain/monger "3.1.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.tiemens/secretshare "1.4.2"]
                 [buddy/buddy-hashers "1.2.0"]
                 [simple-time "0.2.1" :exclusions [joda-time]]
                 [environ "1.1.0"]
                 [clojure-humanize "0.2.2"]
                 [clj.qrgen "0.4.0"]
                 [clavatar "0.3.0"]
                 ;; Gossip is a lein tool to generate call-graphs for Clojure code
                 [cc.artifice/lein-gossip "0.2.1"]
                 [circleci/clj-yaml "0.5.5"]

                 ; fxc secret sharing protocol
                 [org.clojars.dyne/fxc "0.3.0"]

                 ; email
                 [com.draines/postal "2.0.2"]]

  :pedantic? :warn

  :source-paths ["src"]
  :resource-paths ["resources" "test-resources"]
  :jvm-opts ["-Djava.security.egd=file:/dev/random" ;use a proper random source (install haveged)
             "-XX:-OmitStackTraceInFastThrow" ; prevent JVM exceptions without stack trace
             ]
  :env [[:base-url "http://localhost:8000"]

        ;; translation is configured here, strings are hard-coded at compile time
        ;; the last one acts as fallback if translated strings are not found
        [:translation-language "lang/en.yml"]
        [:translation-fallback "lang/en.yml"]]

  :aliases {"dev"  ["with-profile" "dev" "ring" "server"]
            "prod" ["with-profile" "production" "run"]
            "test-transactions" ["with-profile" "transaction-graph" "run"]}
  :profiles {:dev [:dev-common :dev-local]
             :dev-common {:dependencies [[midje "1.8.3"]
                                         [peridot "0.4.4"]
                                         [kerodon "0.8.0"]
                                         [ns-tracker ~ns-tracker-version]]
                          :repl-options {:init-ns freecoin.core}
                          :env [[:base-url "http://localhost:8000"]
                                [:client-id "LOCALFREECOIN"]
                                [:client-secret "FREECOINSECRET"]
                                [:email-config "email-conf.edn"]
                                [:secure "false"]
                                [:admin-email "sender@mail.com"]
                                [:ttl-password-recovery "1800"]]
                          :plugins [[lein-midje "3.1.3"]]}

             :rel [:release :release-local]
             :release {:env [[:base-url "http://demo.freecoin.ch:8000"]
                             [:client-id "dyne-demo-freecoin"]
                             [:client-secret "secret"]
                             [:email-config "email-conf.edn"]
                             [:secure "true"]]}

             :transaction-graph [:dev :user
                                 {:dependencies [[org.clojure/test.check "0.9.0"]]
                                  :source-paths ["src" "scripts"]
                                  :main transaction-graph.main}]
             :production {:source-paths ["src" "prod"]
                          :main freecoin.main
                          ;; TODO replace with scipt
                          :env [[:base-url "http://freecoin1prod.dyne.org:8000"]
                                [:client-id "LOCALFREECOIN"]
                                [:client-secret "FREECOINSECRET"]
                                [:email-config "email-conf.edn"]
                                [:secure "false"]
                                [:ttl-password-recovery "1800"]]}
             :uberjar {:dependencies [[ns-tracker ~ns-tracker-version]]
                       :source-paths ["src" "prod"]
                       :aot :all
                       :main freecoin.main
                       ;; TODO replace with script
                       :env [[:base-url "http://freecoin1staging.dyne.org:8000"]
                             [:client-id "LOCALFREECOIN"]
                             [:client-secret "FREECOINSECRET"]
                             [:email-config "email-conf.edn"]
                             [:secure "false"]
                             [:ttl-password-recovery "1800"]]}
             }
  
  :plugins [[lein-ring "0.9.3"]
            [lein-environ "1.0.0"]]
  :ring {:reload-paths ["src"]
         :init freecoin.core/lein-ring-init
         :handler freecoin.core/lein-ring-server
         :destroy freecoin.core/lein-ring-stop
         :port 8000})
