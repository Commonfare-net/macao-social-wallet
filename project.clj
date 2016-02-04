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
                 [liberator "0.14.0"]
                 [clj-http "2.0.1"]
                 [scenic "0.2.5"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring.middleware.logger "0.5.0"]
                 [compojure "1.4.0"]
                 [org.slf4j/slf4j-simple "1.7.12"]
                 [org.clojars.d-cent/stonecutter-oauth "0.2.0-SNAPSHOT"]
                 [http-kit "2.1.19"]
                 [enlive "1.1.6"]
                 [formidable "0.1.10"]
                 [cheshire "5.4.0"]
                 [json-html "0.2.8"]
                 [autoclave "0.1.7" :exclusions [com.google.guava/guava com.google.code.findbugs/jsr305]]
                 [com.novemberain/monger "2.0.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 [com.tiemens/secretshare "1.3.1"]
                 [buddy/buddy-hashers "0.9.0"]
                 [simple-time "0.2.0"]
                 [environ "1.0.0"]
                 [clojure-humanize "0.1.0"]
                 [clj.qrgen "0.4.0"]
                 [clavatar "0.3.0"]
                 [cc.artifice/lein-gossip "0.2.1"]
                 ]


  :source-paths ["src"]
  :jvm-opts ["-Djava.security.egd=file:/dev/random" ;use a proper random source (install haveged)
             "-XX:-OmitStackTraceInFastThrow" ; prevent JVM exceptions without stack trace
             ] 
  :env [[:base-url "http://localhost:8000"]]
  :aliases {"dev"  ["with-profile" "dev" "ring" "server"]
            "prod" ["with-profile" "production" "run"]
            "test-transactions" ["with-profile" "transaction-graph" "run"]}
  :profiles {:dev [:dev-common :dev-local]
             :dev-common {:dependencies [[midje "1.6.3"]
                                         [peridot "0.3.1"]
                                         [kerodon "0.6.1"]
                                         [ns-tracker "0.3.0"]
                                         ]
                          :env [[:base-url "http://localhost:8000"]
                                [:client-id "LOCALFREECOIN"]
                                [:client-secret "FREECOINSECRET"]
                                [:auth-url "http://localhost:3000"]
                                [:secure "false"]]
                          :plugins [[lein-midje "3.1.3"]]}

             :rel [:release :release-local]
             :release {:env [[:base-url "http://demo.freecoin.ch:8000"]
                             [:client-id "dyne-demo-freecoin"]
                             [:client-secret "secret"]
                             [:auth-url "https://sso.dcentproject.eu"]
                             [:secure "true"]]}

             :transaction-graph [:dev :user
                                 {:dependencies [[org.clojure/test.check "0.8.2"]]
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
