;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Aspasia Beneti <aspra@dyne.org>

;; With contributions by
;; Amy Welch <awelch@thoughtworks.com>
;; Carlo Sciolla <carlo.sciolla@gmail.com>

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

(ns freecoin.test.handlers.transactions
  (:require [midje.sweet :refer :all]
            [freecoin.test.test-helper :as th]
            [freecoin-lib.db.wallet :as w]
            [clj-storage.core :as storage]
            [freecoin-lib.db.confirmation :as c]
            [freecoin-lib.core :as blockchain]
            [freecoin.routes :as routes]
            [freecoin-lib.config :as config]
            [freecoin.handlers.transaction-form :as tf]
            [freecoin.handlers.confirm-transaction-form :as ctf]
            [freecoin.handlers.transactions-list :as tl]
            [freecoin-lib.test-helpers.store :as test-store]
            [ring.mock.request :as rmr]
            [cheshire.core :as cheshire]
            [simple-time.core :as time]
            [taoensso.timbre :as log]
            [just-auth.db.account :as account]
            [auxiliary.translation :as t]))

(def sender-email "sender@mail.com")
(def recipient-email "recipient@mail.com")

(defn setup-with-sender-and-recipient []
  (let [wallet-store (storage/create-memory-store)
        account-store (storage/create-memory-store)
        blockchain (blockchain/create-in-memory-blockchain :bk)
        ;; Sender needs to be an admin otherwise there wont be enough budget to make a transaction
        sender-account (th/create-account account-store {:email sender-email
                                                         :active true
                                                         :flags [:admin]})
        sender-details (w/new-empty-wallet!
                           wallet-store blockchain 
                           "sender" sender-email)
        recipient-details (w/new-empty-wallet!
                           wallet-store blockchain
                           "recipient" recipient-email)]
    
    {:wallet-store wallet-store
     :account-store account-store
     :blockchain blockchain
     :sender-wallet (:wallet sender-details)
     :sender-apikey (:apikey sender-details)
     :recipient-wallet (:wallet recipient-details)
     :recipient-apikey (:apikey recipient-details)}))

(def absolute-path (partial routes/absolute-path))

(against-background [(before :contents (th/init-translation))]
                    (facts "about the create transaction form"
                           (let [{:keys [wallet-store sender-wallet sender-apikey]} (setup-with-sender-and-recipient)
                                 transaction-form-handler (tf/get-transaction-form wallet-store)]
                             (fact "returns 401 when participant is not authenticated"
                                   (-> (th/create-request :get (absolute-path :get-transaction-form)
                                                          {})
                                       transaction-form-handler
                                       :status) => 401)
                             (fact "returns 200 when participant authenticated but without cookie-data"
                                   (-> (th/create-request :get (absolute-path :get-transaction-form)
                                                          {} {:signed-in-email "sender@mail.com"})
                                       transaction-form-handler
                                       :status) => 200)

                             ;; TODO:
                             ;; DM 20150916 - May not be necessary, but perhaps poor UX if
                             ;; user can attempt to make a transaction but fail due to
                             ;; an invalid cookie-data.
                             ;; DM 20151001 - Might be better to prompt user to recover
                             ;; their cookie-data, rather than locking out.
                             (future-fact "cannot be accessed by authenticated user with invalid cookie-data")

                             (fact "returns 200 when participant authenticated with cookie-data"
                                   (let [response (-> (th/create-request :get (absolute-path :get-transaction-form)
                                                                         {} {:signed-in-email sender-email
                                                                             :cookie-data sender-apikey})
                                                      transaction-form-handler)]
                                     (:status response) => 200
                                     (:body response) => (contains #"Make a transaction"))))

                           (future-facts "about redisplaying the form after validation issues"))

                    (facts "about post requests from the transaction form"
                           (let [{:keys [wallet-store account-store sender-wallet sender-apikey]} (setup-with-sender-and-recipient)
                                 blockchain (blockchain/create-in-memory-blockchain :bk)
                                 confirmation-store ...confirmation-store...
                                 form-post-handler (tf/post-transaction-form blockchain wallet-store confirmation-store account-store)]
                             (fact "returns 401 when participant not authenticated"
                                   (-> (th/create-request :post "/post-transaction-form" {})
                                       form-post-handler
                                       :status) => 401)

                             (fact "returns 302 when participant authenticated but without cookie-data"
                                   (-> (th/create-request :post "/post-transaction-form"
                                                          {} {:signed-in-email sender-email})
                                       form-post-handler
                                       :status) => 302)
                             
                             (facts "when participant is authenticated, has cookie-data, and posts a valid form"
                                    (let [confirmation-store (storage/create-memory-store)
                                          form-post-handler (tf/post-transaction-form blockchain wallet-store confirmation-store account-store)
                                          response (-> (th/create-request
                                                        :post "/post-transaction-form"
                                                        {:amount "5.00" :recipient recipient-email}
                                                        {:signed-in-email sender-email :cookie-data sender-apikey})
                                                       form-post-handler)
                                          transaction-confirmation (first (storage/query confirmation-store {}))]
                                      (fact "creates a transaction confirmation"
                                            (test-store/entry-count confirmation-store) => 1)

                                      (fact "redirects to the confirm transaction form"
                                            response => (th/check-redirects-to (absolute-path :get-confirm-transaction-form
                                                                                              :confirmation-uid (:uid transaction-confirmation))))))

                             (facts "when participant tries to make a transaction but doesnt have enough in his wallet"
                                    (let [confirmation-store (storage/create-memory-store)
                                          form-post-handler (tf/post-transaction-form blockchain wallet-store confirmation-store account-store)
                                          response (-> (th/create-request
                                                        :post "/post-transaction-form"
                                                        {:amount "5.00" :recipient recipient-email}
                                                        {:signed-in-email "non-admin@mail.com" :cookie-data sender-apikey})
                                                       form-post-handler)]
                                      (:status response) => 302
                                      (-> response :flash (first) :msg) => "Balance is not sufficient to perform this transaction"))


                             (tabular
                              (fact "redirects to the transaction form page when posted data is invalid"
                                    (let [params (->> {:amount ?amount :recipient ?recipient}
                                                      (filter (comp not nil? val))
                                                      (into {}))
                                          response (-> (th/create-request :post "/post-transaction-form"
                                                                          params
                                                                          {:signed-in-email sender-email
                                                                           :cookie-data sender-apikey})
                                                       form-post-handler)]
                                      response => (th/check-redirects-to (absolute-path :get-transaction-form))))

                              ?amount        ?recipient
                              "0.0"          recipient-email
                              "not-a-float"  recipient-email
                              "5.0"          "nonexistent-email"
                              "5.0"          nil
                              nil            recipient-email)))

                    (facts "about the confirm transaction form"
                           (let [{:keys [wallet-store]} (setup-with-sender-and-recipient)
                                 confirmation-store (storage/create-memory-store)
                                 confirmation (c/new-transaction-confirmation! confirmation-store
                                                                               (constantly "confirmation-uid")
                                                                               sender-email recipient-email 10M)
                                 confirm-transaction-handler (ctf/get-confirm-transaction-form wallet-store confirmation-store)]

                             (fact "displays confirm transaction form"
                                   (let [response
                                         (-> (th/create-request :get "/confirm-transaction/confirmation-uid"
                                                                {:confirmation-uid "confirmation-uid"}
                                                                {:signed-in-email sender-email})
                                             confirm-transaction-handler)]
                                     (:status response) => 200
                                     (:body response) => (contains #"Confirm transaction")))

                             (future-fact "returns 401 when participant not signed in, or does not have apikey")

                             (fact "returns 404 when confirmation does not exist"
                                   (-> (th/create-request :get "/confirm-transaction/nonexisting-confirmation-uid"
                                                          {:confirmation-uid "nonexisting-confirmation-uid"}
                                                          {:signed-in-email sender-email})
                                       confirm-transaction-handler
                                       :status) => 404)

                             (fact "returns 404 when confirmation sender-email does not match signed in user's uid"
                                   (-> (th/create-request :get "/confirm-transaction/confirmation-uid"
                                                          {:confirmation-uid "confirmation-uid"}
                                                          {:signed-in-email "not-the-senders-email"})
                                       confirm-transaction-handler
                                       :status) => 404)

                             (future-fact "returns 404 when confirmation with provided uid is not a transaction confirmation")))

                    (facts "about post requests from the confirm transaction form"
                           (let [{:keys [blockchain wallet-store sender-wallet sender-apikey recipient-wallet]} (setup-with-sender-and-recipient)
                                 confirmation-store (storage/create-memory-store)
                                 form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)]

                             (fact "returns 401 when participant not authenticated"
                                   (-> (th/create-request :post "/post-confirm-transaction-form" {})
                                       form-post-handler
                                       :status) => 401)

                             (fact "returns 401 when participant authenticated but not authorised"
                                   (-> (th/create-request :post "/post-confirm-transaction-form"
                                                          {} {:signed-in-email sender-email})
                                       form-post-handler
                                       :status) => 401)

                             (fact "returns 401 when transaction was not created by the signed-in participant"
                                   (let [confirmation-store (storage/create-memory-store)
                                         confirmation-for-different-sender (c/new-transaction-confirmation! 
                                                                            confirmation-store
                                                                            (constantly "confirmation-for-different-sender-uid")
                                                                            "different-sender@mail.com" recipient-email 10M)
                                         form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)]
                                     (-> (th/create-request :post "/post-confirm-transaction-form"
                                                            {:confirmation-uid "confirmation-for-different-sender-uid"}
                                                            {:signed-in-email sender-email :cookie-data sender-apikey})
                                         form-post-handler
                                         :status)) => 401)

                             (facts "when participant is authenticated and authorised, and is attempting to confirm own transaction"
                                    (let [confirmation-store (storage/create-memory-store)
                                          confirmation (c/new-transaction-confirmation!
                                                        confirmation-store (constantly "confirmation-uid")
                                                        sender-email recipient-email 10M)
                                          form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)
                                          response (-> (th/create-request :post "/post-confirm-transaction-form"
                                                                          {:confirmation-uid "confirmation-uid"}
                                                                          {:signed-in-email sender-email :cookie-data sender-apikey})
                                                       form-post-handler)]

                                      (fact "creates a transaction"
                                            (:transaction-count (test-store/summary blockchain)) => 1)

                                      (fact "deletes the confirmation"
                                            (:entry-count (test-store/summary confirmation-store)) => 0)

                                      (fact "redirects to signed-in participant's account page"
                                            response => (th/check-redirects-to (absolute-path :account :email (:email sender-wallet))))))))

                    (facts "about parsing the tags parameter"
                           (tabular (fact "Tags are optional, and can be provided as one or multiple entries"
                                          (tl/parse-tags ?tags) => ?expected)
                                    ?tags          ?expected
                                    nil            #{}
                                    ""             #{}
                                    "one"          #{"one"}
                                    ["one" "two"]  #{"one" "two"}
                                    ["one" "one"]  #{"one"}
                                    #{"one" "two"} #{"one" "two"})))
