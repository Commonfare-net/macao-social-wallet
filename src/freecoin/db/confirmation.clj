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

(ns freecoin.db.confirmation
  (:require [freecoin.db.mongo :as mongo]
            [freecoin.utils :as util]))

(defn new-transaction-confirmation!
  ([confirmation-store uuid-generator sender-email recipient-email amount]
   (new-transaction-confirmation! confirmation-store uuid-generator sender-email recipient-email amount #{}))
  ([confirmation-store uuid-generator sender-email recipient-email amount tags]
   (let [confirmation {:uid (uuid-generator)
                       :type :transaction
                       :data {:sender-uid sender-email
                              :recipient-uid recipient-email
                              :amount (util/bigdecimal->long amount)
                              :tags tags}}
         stored (some-> (mongo/store! confirmation-store :uid confirmation)
                        (update-in [:data :amount] util/long->bigdecimal))]
     stored)))

(defn fetch [confirmation-store email]
  (some-> (mongo/fetch confirmation-store email)
          (update-in [:data :amount] util/long->bigdecimal)))

(defn delete! [confirmation-store email]
  (mongo/delete! confirmation-store email))
