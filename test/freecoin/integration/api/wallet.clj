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

(defonce app-state {})

(def test-app-params
  {:db-config sh/test-db-config
   :cookie-config {}})

(defn start-test-session [app-state]
  (assoc app-state :session (p/session (core/handler app-state))))

(defn initialise-test-session [app-state params]
  (alter-var-root #'app-state
                  #(-> %
                       (core/init params)
                       core/connect-db
                       start-test-session)))

(defn clear-db [db-connection]
  (do (sh/drop-collection db-connection "wallets")
      (sh/drop-collection db-connection "confirms")
      (sh/drop-collection db-connection "secrets")))

(defn destroy-test-session [app-state]
  (when (:db-connection app-state)
    (clear-db (:db-connection app-state)))
  (alter-var-root #'app-state
                  #(-> %
                       (dissoc :session)
                       core/disconnect-db)))

(def json-body (json/generate-string {:name "user"
                                      :email "test@test.com"}))

(def form-body "name=user&email=test@test.com")

(facts "POST /wallet/create"
       (against-background
        [(before :facts (initialise-test-session app-state test-app-params))
         (after :facts (destroy-test-session app-state))])

       (facts "with content-type application/json"
              (fact "generates a wallet creation confirmation code when provided with unique username and email address"
                    (let [{response :response} (-> (:session app-state)
                                                   (p/content-type "application/json")
                                                   (p/request "/wallet/create"
                                                              :request-method :post
                                                              :body json-body))]
                      (:status response) => 201
                      (:body response) => (ih/json-contains {:body (contains {:name "user"
                                                                              :email "test@test.com"
                                                                              :_id anything})
                                                             :confirm (contains #"^/wallet/create/")})))

              (tabular
               (fact "returns 403 and error report if posted json data is invalid"
                     (let [{response :response} (-> (:session app-state)
                                                    (p/content-type "application/json")
                                                    (p/request "/wallet/create"
                                                               :request-method :post
                                                               :body (json/generate-string ?user-data)))]
                      (:status response) => 403
                      (:body response) => (ih/json-contains ?problems)))

               ?user-data                     ?problems
               {:email "valid@email.com"}     {:reason [{:keys ["name"] :msg "must not be blank"}]})
              
              (fact "returns 403 if a wallet already exists for the given username"
                    (let [wallet (storage/insert (:db-connection app-state) "wallets" {:name "user"})
                          {response :response} (-> (:session app-state)
                                                   (p/content-type "application/json")
                                                   (p/request "/wallet/create"
                                                              :request-method :post
                                                              :body json-body))]
                      (:status response) => 403
                      (:body response) => (ih/json-contains {:reason [{:keys ["name"] :msg "Username already exists"}]}))))
       
       (future-facts "with content-type application/x-www-form-urlencoded"
                     (fact "when successful, initiates confirmation process")
                     (fact "when posted data is invalid, redirects to /wallet/create with error message")
                     (fact "when username not unique, redirects to /wallet/create with error message")))
