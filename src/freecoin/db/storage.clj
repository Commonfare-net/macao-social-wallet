;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2016 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.db.storage
  (:require [freecoin.db.mongo :as m]))

(defn create-mongo-stores [db]
  {:wallet-store       (m/create-wallet-store db)
   :confirmation-store (m/create-confirmation-store db)
   :transaction-store  (m/create-transaction-store db)
   :account-store (m/create-account-store db)
   :tag-store (m/create-tag-store db)})

(defn create-in-memory-stores []
  {:wallet-store       (m/create-memory-store)
   :confirmation-store (m/create-memory-store)
   :transaction-store (m/create-memory-store)
   :account-store (m/create-memory-store)})

(defn get-wallet-store [stores-m]
  (:wallet-store stores-m))

(defn get-confirmation-store [stores-m]
  (:confirmation-store stores-m))

(defn get-transaction-store [stores-m]
  (:transaction-store stores-m))

(defn get-account-store [stores-m]
  (:account-store stores-m))

(defn get-tag-store [stores-m]
  (:tag-store stores-m))

(defn empty-db-stores! [stores-m]
  (m/delete-all! (get-wallet-store stores-m))
  (m/delete-all! (get-confirmation-store stores-m))
  (m/delete-all! (get-transaction-store stores-m))
  (m/delete-all! (get-account-store stores-m))
  (m/delete-all! (get-tag-store stores-m)))
