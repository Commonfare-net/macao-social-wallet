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

(ns freecoin.test.db.confirmation
  (:require [midje.sweet :refer :all]
            [freecoin.db.mongo :as fm]
            [freecoin.db.confirmation :as confirmation]
            [freecoin.test-helpers.store :as test-store]))

(def sender-email "sender@email.com")
(def recipient-email "recipient@email.com")

(facts "Can create and fetch a transaction confirmation"
       (let [uuid-generator (constantly "a-uuid")
             confirmation-store (fm/create-memory-store)]
         (fact "can create a transaction confirmation"
               (let [confirmation (confirmation/new-transaction-confirmation! confirmation-store uuid-generator
                                                                              sender-email recipient-email 10M)]
                 confirmation => (just {:uid "a-uuid"
                                        :type :transaction
                                        :data {:sender-email sender-email
                                               :recipient-email recipient-email
                                               :amount 10M
                                               :tags #{}}})))

         (fact "can fetch a transaction confirmation by its uid"
               (confirmation/fetch confirmation-store "a-uuid")
               => (just {:uid "a-uuid"
                         :type :transaction
                         :data {:sender-email sender-email
                                :recipient-email recipient-email
                                :amount 10M
                                :tags #{}}}))

         (fact "transaction confirmations can have tags"
               (let [confirmation (confirmation/new-transaction-confirmation! confirmation-store
                                                                              uuid-generator
                                                                              sender-email
                                                                              recipient-email
                                                                              10M
                                                                              #{:air-drop})]
                 confirmation => (just {:uid "a-uuid"
                                        :type :transaction
                                        :data {:sender-email sender-email
                                               :recipient-email recipient-email
                                               :amount 10M
                                               :tags #{:air-drop}}})))))

(fact "Can delete a confirmation"
      (let [confirmation-store (fm/create-memory-store)
            confirmation (confirmation/new-transaction-confirmation! confirmation-store (constantly "uid")
                                                                     sender-email recipient-email 10M)]
        (test-store/summary confirmation-store) => (contains {:entry-count 1})
        (confirmation/delete! confirmation-store "uid")
        (test-store/summary confirmation-store) => (contains {:entry-count 0})))
