(ns freecoin.db.storage
  (:require [freecoin.db.mongo :as m]))

(defn create-mongo-stores [db]
  {:wallet-store       (m/create-wallet-store db)
   :confirmation-store (m/create-confirmation-store db)})

(defn create-in-memory-stores []
  {:wallet-store       (m/create-memory-store)
   :confirmation-store (m/create-memory-store)})

(defn get-wallet-store [stores-m]
  (:wallet-store stores-m))

(defn get-confirmation-store [stores-m]
  (:confirmation-store stores-m))
