(ns freecoin.config
  (:require [environ.core :as env]))

(def env-vars #{:port :host :base-url
                :client-id :client-secret :auth-url})

(defn create-config []
  (select-keys env/env env-vars))

(defn get-env
  "Like a normal 'get' except it also ensures the key is in the env-vars set"
  ([config-m key]
   (get config-m (env-vars key)))
  ([config-m key default]
   (get config-m (env-vars key) default)))

(defn port [config-m]
  (Integer. (get-env config-m :port "8000")))

(defn host [config-m]
  (get-env config-m :host "localhost"))

(defn base-url [config-m]
  (get-env config-m :base-url "http://localhost:8000"))

(defn client-id [config-m]
  (get-env config-m :client-id))

(defn client-secret [config-m]
  (get-env config-m :client-secret))

(defn auth-url [config-m]
  (get-env config-m :auth-url))

(defn cookie-secret [config-m]
  (get-env config-m :cookie-secret "encryptthecookie"))

(defn- get-docker-mongo-uri [config-m]
  (when-let [mongo-ip (get-env config-m :mongo-port-27017-tcp-addr)]
    (format "mongodb://%s:27017/freecoin" mongo-ip)))

(defn mongo-uri [config-m]
  (or (get-docker-mongo-uri config-m)
      (get-env config-m :mongo-uri)
      "mongodb://localhost:27017/freecoin"))
