(ns freecoin.test-helpers.integration
  (:require [monger.core :as monger]
            [freecoin.core :as core]
            [freecoin-lib.core :as blockchain]
            [freecoin-lib.db.freecoin :as db]
            [just-auth.core :as auth])
  (:import [freecoin_lib.core InMemoryBlockchain]))

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
  (let [stores (db/create-in-memory-stores)]
    {:stores-m stores
     :blockchain (blockchain/create-in-memory-blockchain :bk)
     :config-m {:secure "false"
                :email-config "email-conf.edn"}
     :email-activator (auth/->StubEmailBasedAuthentication (atom []) (:account-store stores))
     :password-recoverer (auth/->StubEmailBasedAuthentication (atom []) (:password-recovery-store stores))}))

(defn build-app [app-config-override-m]
  (let [{:keys [config-m stores-m blockchain email-activator password-recoverer]} (merge (default-app-config-m) app-config-override-m)]
    (core/create-app config-m stores-m blockchain email-activator password-recoverer)))
