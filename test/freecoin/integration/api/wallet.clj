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
(ns freecoin.integration.api.wallet
  (:require [midje.sweet :refer :all]
            [peridot.core :as p]
            [cheshire.core :as json]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.integration.integration-helpers :as ih]
            [freecoin.core :as core]
            [freecoin.storage :as storage]))

(def json-body (json/generate-string {:name "user"
                                      :email "test@test.com"}))

(def form-body "name=user&email=test@test.com")

(facts "POST /signin"
       (against-background
        [(before :facts (ih/initialise-test-session ih/app-state ih/test-app-params))
         (after :facts (ih/destroy-test-sessions ih/app-state))])

       (facts "with content-type application/json"
              (fact "when successful, generates a wallet creation confirmation code"
                    (let [{response :response} (-> (get-in ih/app-state [:sessions :default])
                                                   (p/content-type "application/json")
                                                   (p/request "/signin"
                                                              :request-method :post
                                                              :body json-body))]
                      (:status response) => 201
                      (:body response) => (ih/json-contains
                                           {:status 200
                                            :location (contains #"^/confirmations/")}
                                           )))

              (tabular
               (fact "returns 403 and error report if posted json data is invalid"
                     (let [{response :response} (-> (get-in ih/app-state [:sessions :default])
                                                    (p/content-type "application/json")
                                                    (p/request "/signin"
                                                               :request-method :post
                                                               :body (json/generate-string ?user-data)))]
                      (:status response) => 403
                      (:body response) => (ih/json-contains ?problems)))

               ?user-data                      ?problems
               {:email "valid@email.com"}      {:reason [{:keys ["name"] :msg "must not be blank"}]}
               {:name "user"}                  {:reason [{:keys ["email"] :msg "must not be blank"}]}
               {:name "user" :email "invalid"} {:reason [{:keys ["email"] :msg "must be a valid email"}]})

              (fact "returns 403 if a wallet already exists for the given username"
                    (let [wallet (storage/insert (:db (:db-connection ih/app-state)) "wallets" {:name "user"})
                          {response :response} (-> (get-in ih/app-state [:sessions :default])
                                                   (p/content-type "application/json")
                                                   (p/request "/signin"
                                                              :request-method :post
                                                              :body json-body))]
                      (:status response) => 403
                      (:body response) => (ih/json-contains {:reason [{:keys ["name"] :msg "username already exists"}]}))))

       (facts "with content-type application/x-www-form-urlencoded"
              (fact "when successful, initiates confirmation process"
                    (let [{response :response} (-> (get-in ih/app-state [:sessions :default])
                                                   (p/content-type "application/x-www-form-urlencoded")
                                                   (p/request "/signin"
                                                              :request-method :post
                                                              :body form-body))]
                      (:status response) => 303
                      (:headers response) => (contains {"Location" #"^/confirmations/"})))

              (tabular
               (fact "when posted data is invalid, redirects to /signin with error message"
                     (let [{response :response} (-> (get-in ih/app-state [:sessions :default])
                                                    (p/content-type "application/x-www-form-urlencoded")
                                                    (p/request "/signin"
                                                               :request-method :post
                                                               :body ?user-data))]
                       (:status response) => 403
                       (:body response) => (contains ?error-text)))

               ?user-data                     ?error-text
               "name=user"                    #"Email.+: must not be blank"
               "email=valid@email.com"        #"Name.+: must not be blank"
               "name=user&email=invalid"      #"Email.+: must be a valid email")

              (fact "when username not unique, redirects to /signin with error message"
                    (let [wallet (storage/insert (:db (:db-connection ih/app-state)) "wallets" {:name "user"})
                          {response :response} (-> (get-in ih/app-state [:sessions :default])
                                                   (p/content-type "application/x-www-form-urlencoded")
                                                   (p/request "/signin"
                                                              :request-method :post
                                                              :body "name=user&email=valid@email.com"))]
                      (:status response) => 403
                      (:body response) => (contains #"Name.+: username already exists")))))
