;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Duncan Mortimer <dmortime@thoughtworks.com>

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

(ns freecoin.db.wallet
  (:require [freecoin.db.uuid :as uuid]
            [freecoin.blockchain :as blockchain]
            [freecoin.db.mongo :as mongo]))

(def empty-wallet {:public-key nil
                   :private-key nil
                   :blockchains {}
                   :blockchain-secrets {}})

(defn new-empty-wallet! [wallet-store sso-id name email]
  (let [participant {:name name :email email}
        wallet (-> empty-wallet
                   (assoc :uid (uuid/uuid))
                   (assoc :participant participant)
                   (assoc :sso-id sso-id))]
    (mongo/store! wallet-store :uid wallet)))

(defn fetch [wallet-store uid]
  (mongo/fetch wallet-store uid))

(defn fetch-by-sso-id [wallet-store sso-id]
  (first (mongo/query wallet-store {:sso-id sso-id})))

(defn add-blockchain-to-wallet-with-id! [wallet-store blockchain uid]
  (mongo/update! wallet-store uid (partial blockchain/create-account blockchain)))
