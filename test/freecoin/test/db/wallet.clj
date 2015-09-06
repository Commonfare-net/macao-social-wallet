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

(ns freecoin.test.db.wallet
  (:require [midje.sweet :refer :all]
            [freecoin.db.uuid :as uuid]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as wallet]))

(facts "can create and fetch an empty wallet"
       (against-background (uuid/uuid) => "a-uuid")
       (let [wallet-store (fm/create-memory-store)]
         (wallet/new-empty-wallet! wallet-store) => (just {:uid "a-uuid"
                                                           :public-key nil
                                                           :private-key nil
                                                           :blockchains {}
                                                           :blockchain-secrets {}})
         (wallet/fetch wallet-store "a-uuid") => (just {:uid "a-uuid"
                                                        :public-key nil
                                                        :private-key nil
                                                        :blockchains {}
                                                        :blockchain-secrets {}})))
