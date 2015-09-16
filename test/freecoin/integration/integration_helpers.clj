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
(ns freecoin.integration.integration-helpers
  (:require [midje.sweet :as midje]
            [clojure.data.json :as cl-json]
            [peridot.core :as p]
            [ring.middleware.session.store :as rms]
            [clojure.string :as str]
            [freecoin.storage :as storage]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as wallet]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.blockchain :as blockchain]
            [freecoin.core :as core]))

;; For whole-stack integration tests
(defonce app-state {})

(def test-app-params
  {:db-config sh/test-db-config
   :cookie-config {}})

(deftype TestSessionStore [store-atom]
  rms/SessionStore
  (read-session [_ key] @store-atom)
  (write-session [_ _ data]
    (reset! store-atom data)
    :key)
  (delete-session [_ _]
    (reset! store-atom nil)
    nil))

(defn test-session-store [store-atom]
  (TestSessionStore. store-atom))

(defn start-test-session [app-state session-label]
  (let [store-atom (atom nil)
        session-configuration {:store (test-session-store store-atom)}
        db-connection (:db-connection app-state)]
    (-> app-state
        (assoc-in [:sessions session-label]
                  (p/session (core/handler session-configuration db-connection {})))
        (assoc-in [:session-stores session-label] store-atom))))

(defn initialise-test-session
  ([app-state params]
   (initialise-test-session app-state params :default))

  ([app-state params session-label]
   (alter-var-root #'app-state
                   #(-> %
                        (core/init params)
                        core/connect-db
                        (start-test-session session-label)))))

(defn destroy-test-sessions [app-state]
  (when (:db-connection app-state)
    (sh/clear-db (:db-connection app-state)))
  (alter-var-root #'app-state
                  #(-> %
                       (dissoc :sessions)
                       core/disconnect-db)))

(defn create-and-sign-in [app-state session-label wallet-details]
  (let [{:keys [sso-id name email]} wallet-details
        db (:db-connection app-state)
        wallet-store (fm/create-wallet-store (:db db))
        blockchain (blockchain/new-stub db)
        session-store (get-in app-state [:session-stores session-label])
        {:keys [wallet apikey]} (wallet/new-empty-wallet! wallet-store blockchain sso-id name email)]
    (reset! session-store {:signed-in-uid (:uid wallet)
                           :cookie-data apikey})
    app-state))

;; Midje checkers

(defn parse-json-string-with-entity-value-as-keyword [json-string]
  (cl-json/read-str json-string 
                    :key-fn keyword 
                    :value-fn (fn [k v] ((if (= k :entity) keyword identity) v))))

(defn json-contains [expected & options]
  (midje/chatty-checker [actual]
                         ((apply midje/contains expected options) 
                          (parse-json-string-with-entity-value-as-keyword actual))))

