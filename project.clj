(def ns-tracker-version "0.3.1")

(defproject org.dyne/freecoin "0.4.0-SNAPSHOT"
  :description "Freecoin digital currency toolkit"
  :url "https://freecoin.dyne.org"

  :license {:author "Dyne.org Foundation"
            :email "foundation@dyne.org"
            :year 2017
            :key "gpl-3.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [liberator "0.15.1" :exclusions [hiccup]]
                 [clj-http "3.6.1"]
                 [scenic "0.2.5"]
                 [ring/ring-core "1.6.2"]
                 [ring/ring-anti-forgery "1.1.0"]
                 [ring/ring-defaults "0.3.0"]
                 [ring.middleware.logger "0.5.0" :exclusions [org.slf4j/slf4j-api]]

                 [http-kit "2.2.0"]
                 [enlive "1.1.6"]
                 [formidable "0.1.10"]
                 [cheshire "5.7.1"]
                 [json-html "0.4.4"]

                 [buddy/buddy-hashers "1.3.0"]
                 [environ "1.1.0"]
                 [clojure-humanize "0.2.2"]

                 ;; qrcode generation and scanning
                 [clj.qrgen "0.4.0"]
                 [com.google.zxing/core "3.2.1"]
                 
                 [clavatar "0.3.0"]
                 [circleci/clj-yaml "0.5.6"]

                 ; fxc secret sharing protocol
                 [org.clojars.dyne/fxc "0.5.0"]

                 ; freecoin core lib
                 [org.clojars.dyne/freecoin-lib "0.8.0-SNAPSHOT"]

                 ;; 2 step authentication
                 [org.clojars.dyne/just-auth "0.1.0-SNAPSHOT"]

                 ;;error handling
                 [failjure "1.2.0"]]

  :pedantic? :warn

  :source-paths ["src"]
  :resource-paths ["resources" "test-resources"]
  :jvm-opts ["-Djava.security.egd=file:/dev/random" ;use a proper random source (install haveged)
             "-XX:-OmitStackTraceInFastThrow" ; prevent JVM exceptions without stack trace
             ]
  :env [

        ;; translation is configured here, strings are hard-coded at compile time
        ;; the last one acts as fallback if translated strings are not found
        [:translation-language "lang/en.yml"]
        [:translation-fallback "lang/en.yml"]]

  :aliases {"dev"  ["with-profile" "dev" "ring" "server"]
            "prod" ["with-profile" "production" "run"]
            "test-transactions" ["with-profile" "transaction-graph" "run"]
            "test-basic" ["midje" ":config" "test-resources/fast-tests.config"]}
  :profiles {:dev [:dev-common :dev-local]
             :dev-common {:dependencies [[midje "1.8.3"]
                                         [peridot "0.4.4"]
                                         [kerodon "0.8.0"]
                                         [ns-tracker ~ns-tracker-version]]
                          :repl-options {:init-ns freecoin.core}
                          :env [[:base-url "http://localhost:8000"]
                                [:email-config "email-conf.edn"]
                                [:secure "false"] 
                                [:ttl-password-recovery "1800"]]
                          :plugins [[lein-midje "3.1.3"]]}

             :rel [:release :release-local]
             :release {:env [[:base-url "http://demo.freecoin.ch:8000"]
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
                                [:email-config "email-conf.edn"]
                                [:secure "false"]
                                [:ttl-password-recovery "1800"]]}
             :uberjar {:dependencies [[ns-tracker ~ns-tracker-version]]
                       :source-paths ["src" "prod"]
                       :aot :all
                       :main freecoin.main
                       ;; TODO replace with script
                       :env [[:base-url "http://freecoin1staging.dyne.org:8000"]
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
