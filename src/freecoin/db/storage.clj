(ns freecoin.db.storage
  (:require [freecoin.db.mongo :as m]))

(defn create-mongo-stores [db]
  {:wallet-store       (m/create-wallet-store db)
   :confirmation-store (m/create-confirmation-store db)
   :transaction-store  (m/create-transaction-store db)
   :account-store (m/create-account-store db)})

(defn create-in-memory-stores []
  {:wallet-store       (m/create-memory-store)
   :confirmation-store (m/create-memory-store)
   :transaction-store (m/create-memory-store)
   :account-store (m/create-account-store)})

(defn get-wallet-store [stores-m]
  (:wallet-store stores-m))

(defn get-confirmation-store [stores-m]
  (:confirmation-store stores-m))

(defn get-transaction-store [stores-m]
  (:transaction-store stores-m))

(defn get-account-store [stores-m]
  (:account-store stores-m))

(defn empty-db-stores [stores-m]
  (m/delete-all! (get-wallet-store stores-m))
  (m/delete-all! (get-confirmation-store stores-m))
  (m/delete-all! (get-transaction-store stores-m))
  (m/delete-all! (get-account-store stores-m)))
