;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti  <aspra@dyne.org>

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

(ns freecoin.db.account
  (:require [freecoin.db.mongo :as mongo]))

(defn new-account!
  [account-store {:keys [first-name last-name email password] :as account-map}]
  (mongo/store! account-store :email (assoc account-map :activated false)))


(defn activate! [account-store email]
  (mongo/update! account-store email #(assoc % :activated true)))

(defn fetch [account-store email]
  (some-> (mongo/fetch account-store email)
          ;; TODO password encr
          ))

(defn fetch-by-activation-id [account-store activation-id]
  (first (mongo/query account-store {:activation-id activation-id})))

(defn update-activation-id! [account-store email activation-link]
  (mongo/update! account-store email #(assoc % :activation-id activation-link)))

(defn delete! [account-store email]
  (mongo/delete! account-store email))
