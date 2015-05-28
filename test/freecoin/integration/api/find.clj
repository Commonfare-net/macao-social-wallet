;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>
;; Andrei Biasprozvanny <abiaspro@thoughtworks.com>

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
(ns freecoin.integration.api.find
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.integration.integration-helpers :as ih]
            [freecoin.core :as core]
            [freecoin.storage :as storage]
            [freecoin.auth :as auth]))

(def wallet
  {:name "user" :email "valid@email.com"})

(facts "GET /find/:field/:value"
       (against-background
        [(before :facts (ih/initialise-test-session ih/app-state ih/test-app-params))
         (after :facts (ih/destroy-test-session ih/app-state))])

       (fact "Responds with 40? when user is not authenticated"
             (let [{response :response} (-> (:session ih/app-state)
                                            (p/request "/find/name/user"))]
               (:status response) => 401
               (:body response) => (contains #"Sorry, you are not signed in")
               )
             )

       (facts "when user is authenticated"
              (against-background
               (auth/check anything) => {:authorised? true})

              (tabular
               (fact "Retrieves a wallet by name or by email address"
                     (let [_ (storage/insert (:db-connection ih/app-state) "wallets" wallet)
                           {response :response} (-> (:session ih/app-state)
                                                    (p/request (str "/find/" ?field "/" ?value)))]
                       (:status response) => 200
                       (:body response) => (contains #"name.+user")
                       (:body response) => (contains #"email.+valid@email.com")))

               ?field   ?value               ?result
               "name"   "user"               wallet
               "email"  "valid@email.com"    wallet)

              (fact "Responds with a 200 and reports no result when no wallets found"
                    (let [{response :response} (-> (:session ih/app-state)
                                                   (p/request (str "/find/name/user")))]
                      (:status response) => 200
                      (:body response) => (contains #"address.+not found")))))
