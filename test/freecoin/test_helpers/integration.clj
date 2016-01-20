(ns freecoin.test-helpers.integration
  (:require [monger.core :as monger]
            [freecoin.core :as core]
            [freecoin.blockchain :as blockchain]
            [freecoin.db.storage :as s]))

(def test-db-name "freecoin-test-db")
(def test-db-uri (format "mongodb://localhost:27017/%s" test-db-name))

(def db-and-conn (atom nil))

(defn get-test-db []
  (:db @db-and-conn))

(defn get-test-db-connection []
  (:conn @db-and-conn))

(defn- drop-db [db-and-conn]
  (monger/drop-db (:conn db-and-conn) test-db-name)
  db-and-conn)

(defn setup-db []
  (->> (monger/connect-via-uri test-db-uri)
       drop-db
       (reset! db-and-conn)))

(defn reset-db []
  (drop-db @db-and-conn))

(defn teardown-db []
  (drop-db @db-and-conn)
  (monger/disconnect (get-test-db-connection))
  (reset! db-and-conn nil))

(defn default-app-config-m []
  {:stores-m (s/create-in-memory-stores)
   :blockchain (blockchain/create-in-memory-blockchain :bk)
   :config-m {:secure "false"
              :client-secret "freecoin-secret"
              :client-id "freecoin"
              :auth-url "stonecutter-url"}})

(defn build-app [app-config-override-m]
  (let [{:keys [config-m stores-m blockchain]} (merge (default-app-config-m) app-config-override-m)]
    (core/create-app config-m stores-m blockchain)))
