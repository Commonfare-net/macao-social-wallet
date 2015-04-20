(defproject freecoin "0.2.0-SNAPSHOT"
  :description "Freecoin digital currency toolkit"
  :url "http://freecoin.ch"
  :license {:name "GNU GPL Affero v3 and "
            :url "http://www.d-centproject.eu"}

  :profiles {:dev {:dependencies [[midje "1.6.3"]]}}

  :plugins [
            [lein-ring "0.9.3"]
            [lein-environ "1.0.0"]
            ]

  :ring {:handler freecoin.core/handler}

  :source-paths ["src"]

  ;; :java-source-paths ["lib/java"]
  ;; :target-path "target/%s/"
  ;; :javac-options ["-target" "1.9" "-source" "1.9"])

  ;; make sure we use a proper source of random
  :jvm-opts ["-Djava.security.egd=file:/dev/random"]

  :dependencies [
                 [org.clojure/clojure "1.6.0"]

                 ;; rest api
                 [liberator "0.12.2"]

                 ;; liberator
                 [compojure "1.3.3"]
                 [ring/ring-core "1.3.2"]

                 ;; http client
                 [http-kit "2.1.18"]

                 ;; templating
                 [enlive "1.1.5"]

                 ;; json marshalling
                 [cheshire "5.4.0"]

                 ;; encryption
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [com.tiemens/secretshare "1.3.1"]
                 [jstrutz/hashids "1.0.1"]

                 ;; introspection
                 [org.clojure/tools.namespace "0.2.10"]

                 ;; configuration
                 [environ "0.5.0"]
                 ]
  :env [
        [:base-url "http://localhost:3000"]
        ]
  )

  
