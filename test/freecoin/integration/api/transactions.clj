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
(ns freecoin.integration.api.transactions
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.integration.integration-helpers :as ih]
            [freecoin.core :as core]
            [freecoin.storage :as storage]))

(def donor-wallet
  {:name "donor" :email "donor@email.com"})

(def recipient-wallet
  {:name "recipient" :email "recipient@email.com"})

(def json-body (json/generate-string {:recipient "recipient"
                                      :amount "10.0"}))

(def form-body "recipient=recipient&amount=10.0")

(facts "POST /send"
       (against-background
        [(before :facts (do (ih/initialise-test-session ih/app-state ih/test-app-params :donor)
                            (ih/initialise-test-session ih/app-state ih/test-app-params :recipient)
                            (ih/create-and-sign-in ih/app-state :donor donor-wallet)
                            (ih/create-and-sign-in ih/app-state :recipient recipient-wallet)))
         (after :facts (ih/destroy-test-sessions ih/app-state))])
       
       (facts "with content-type application/json"
              (fact "when successful, creates a transaction confirmation resource"
                    (let [{response :response} (-> (get-in ih/app-state [:sessions :donor])
                                                   (p/content-type "application/json")
                                                   (p/request "/send"
                                                              :request-method :post
                                                              :body json-body))]
                      (:status response) => 201
                      (:headers response) => (contains {"Location" #"^/confirmations/"})))

              (tabular
               (fact "returns 403 and error report if posted json data is invalid"
                     (let [{response :response} (-> (get-in ih/app-state [:sessions :donor])
                                                    (p/content-type "application/json")
                                                    (p/request "/send"
                                                               :request-method :post
                                                               :body (json/generate-string ?transaction-data)))]
                       (:status response) => 403
                       (:body response) => (ih/json-contains ?problems)))
               ?transaction-data                        ?problems
               {:recipient "no-one" :amount "10.00"}    {:reason [{:keys ["recipient"]
                                                                   :msg "there is no recipient with that name"}]}
               {:recipient "recipient"}                 {:reason [{:keys ["amount"]
                                                                   :msg "must not be blank"}]}
               {:recipient "recipient" :amount "-10.0"} {:reason [{:keys ["amount"]
                                                                   :msg "cannot be less than 0.01"}]}
               {:amount "10.0"}                         {:reason [{:keys ["recipient"]
                                                                   :msg "must not be blank"}]}))

       (facts "with content-type application/x-www-form-urlencoded"
              (fact "when successful, redirects to a confirmation form"
                    (let [{response :response} (-> (get-in ih/app-state [:sessions :donor])
                                                   (p/content-type "application/x-www-form-urlencoded")
                                                   (p/request "/send"
                                                              :request-method :post
                                                              :body form-body))]
                      (:status response) => 303
                      (:headers response) => (contains {"Location" #"^/confirmations/"})))

              (tabular
               (fact "returns 403 and error report if posted json data is invalid"
                     (let [{response :response} (-> (get-in ih/app-state [:sessions :donor])
                                                    (p/content-type "application/x-www-form-urlencoded")
                                                    (p/request "/send"
                                                               :request-method :post
                                                               :body ?transaction-data))]
                       (:status response) => 403
                       (:body response) => (contains ?error-text)))
               ?transaction-data                  ?error-text
               "recipient=recipient"              #"Amount.+: must not be blank"
               "recipient=recipient&amount=-10.0" #"Amount.+: cannot be less than 0.01"
               "amount=10.0"                      #"Recipient.+: must not be blank"
               "recipient=no-one&amount=10.0"     #"Recipient.+: there is no recipient with that name")))


