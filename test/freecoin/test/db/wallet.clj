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
            [freecoin.db.mongo :as fm]
            [freecoin.blockchain :as fb]
            [freecoin.db.wallet :as wallet]))

(facts "Can create and fetch an empty wallet"
       (let [wallet-store (fm/create-memory-store)
             blockchain (fb/create-in-memory-blockchain :bk)]
         (fact "can create a wallet"
               (let [{:keys [wallet apikey]} (wallet/new-empty-wallet! wallet-store blockchain
                                                                       "sso-id" "name" "test@email.com")]
                 wallet => (just {:sso-id "sso-id"
                                  :name "name"
                                  :email "test@email.com"
                                  :public-key nil
                                  :private-key nil
                                  :account-id anything})))
         
         (fact "can fetch the wallet by its email"
               (wallet/fetch wallet-store "test@email.com")
               => (just {:sso-id "sso-id"
                         :name "name"
                         :email "test@email.com"
                         :public-key nil
                         :private-key nil
                         :account-id anything}))
         
         (fact "can fetch wallet by sso-id"
               (wallet/fetch-by-sso-id wallet-store "sso-id")
               => (just {:sso-id "sso-id"
                         :name "name"
                         :email "test@email.com"
                         :public-key nil
                         :private-key nil
                         :account-id anything}))))

(defn create-wallet [wallet-store blockchain wallet-data]
  (let [{:keys [sso-id name email]} wallet-data]
    (:wallet (wallet/new-empty-wallet! wallet-store blockchain sso-id name email))))

(defn populate-wallet-store [wallet-store blockchain]
  (let [wallets-data [{:name "James Jones" :email "james@jones.com" :sso-id "sso-id-1"}
                      {:name "James Jones" :email "jim@jones.com" :sso-id "sso-id-2"}
                      {:name "Sarah Lastname" :email "sarah@email.com" :sso-id "sso-id-3"}]]
    (doall (map (partial create-wallet wallet-store blockchain) wallets-data))))

(facts "Can query wallet collection"
       (let [wallet-store (fm/create-memory-store)
             blockchain (fb/create-in-memory-blockchain :bk)
             wallets (populate-wallet-store wallet-store blockchain)]
         
         (fact "without parameters, returns all wallets"
               (wallet/query wallet-store) => (n-of anything 3))

         (tabular
          (fact "accepts an optional query map argument"
                (wallet/query wallet-store ?query-m) => (n-of anything ?expected-count))
          ?query-m                                      ?expected-count
          {:name "James Jones"}                         2
          {:name "Sarah Lastname"}                      1
          {:name "Bob Nothere"}                         0
          {:email "james@jones.com"}                    1
          {:email "bob@nothere.com"}                    0
          {:name "James Jones" :email "jim@jones.com"}  1
          {:something "else"}                           0)))
