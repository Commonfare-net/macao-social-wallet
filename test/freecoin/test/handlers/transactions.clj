;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Amy Welch <awelch@thoughtworks.com>

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
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.db.confirmation :as c]
            [freecoin.blockchain :as fb]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.handlers.transaction-form :as tf]
            [freecoin.handlers.confirm-transaction-form :as ctf]
            [freecoin.handlers.transactions-list :as tl]
            [freecoin.test-helpers.store :as test-store]
            [ring.mock.request :as rmr]
            [freecoin.blockchain :as blockchain]
            [cheshire.core :as cheshire]
            [simple-time.core :as time]))

(defn setup-with-sender-and-recipient []
  (let [wallet-store (fm/create-memory-store)
        blockchain (fb/create-in-memory-blockchain :bk)
        sender-details (w/new-empty-wallet!
                        wallet-store blockchain
                        (constantly "sender-uid")
                        "sender-sso-id" "sender" "sender@email.com")
        recipient-details (w/new-empty-wallet!
                           wallet-store blockchain
                           (constantly "recipient-uid")
                           "recipient-sso-id" "recipient" "recipient@email.com")]
    {:wallet-store wallet-store
     :blockchain blockchain
     :sender-wallet (:wallet sender-details)
     :sender-apikey (:apikey sender-details)
     :recipient-wallet (:wallet recipient-details)
     :recipient-apikey (:apikey recipient-details)}))

(def absolute-path (partial routes/absolute-path (config/create-config)))

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
                                      {} {:signed-in-uid "sender-uid"})
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
                                                     {} {:signed-in-uid "sender-uid"
                                                         :cookie-data sender-apikey})
                                  transaction-form-handler)]
                 (:status response) => 200
                 (:body response) => (contains #"Make a transaction"))))

       (future-facts "about redisplaying the form after validation issues"))

(facts "about post requests from the transaction form"
       (let [{:keys [wallet-store sender-wallet sender-apikey]} (setup-with-sender-and-recipient)
             confirmation-store ...confirmation-store...
             form-post-handler (tf/post-transaction-form wallet-store confirmation-store)]
         (fact "returns 401 when participant not authenticated"
               (-> (th/create-request :post "/post-transaction-form" {})
                   form-post-handler
                   :status) => 401)

         (fact "returns 302 when participant authenticated but without cookie-data"
               (-> (th/create-request :post "/post-transaction-form"
                                      {} {:signed-in-uid "sender-uid"})
                   form-post-handler
                   :status) => 302)

         (facts "when participant is authenticated, has cookie-data, and posts a valid form"
                (let [confirmation-store (fm/create-memory-store)
                      form-post-handler (tf/post-transaction-form wallet-store confirmation-store)
                      response (-> (th/create-request
                                    :post "/post-transaction-form"
                                    {:amount "5.00" :recipient "recipient"}
                                    {:signed-in-uid "sender-uid" :cookie-data sender-apikey})
                                   form-post-handler)
                      transaction-confirmation (first (fm/query confirmation-store {}))]
                  (fact "creates a transaction confirmation"
                        (test-store/entry-count confirmation-store) => 1)

                  (fact "redirects to the confirm transaction form"
                        response => (th/check-redirects-to (absolute-path :get-confirm-transaction-form
                                                                          :confirmation-uid (:uid transaction-confirmation))))))

         (tabular
          (fact "redirects to the transaction form page when posted data is invalid"
                (let [params (->> {:amount ?amount :recipient ?recipient}
                                  (filter (comp not nil? val))
                                  (into {}))
                      response (-> (th/create-request :post "/post-transaction-form"
                                                      params
                                                      {:signed-in-uid "sender-uid"
                                                       :cookie-data sender-apikey})
                                   form-post-handler)]
                  response => (th/check-redirects-to (absolute-path :get-transaction-form))))

          ?amount        ?recipient
          "0.0"          "recipient-uid"
          "not-a-float"  "recipient-uid"
          "5.0"          "nonexistent-uid"
          "5.0"          nil
          nil            "recipient-uid")))

(facts "about the confirm transaction form"
       (let [{:keys [wallet-store]} (setup-with-sender-and-recipient)
             confirmation-store (fm/create-memory-store)
             confirmation (c/new-transaction-confirmation! confirmation-store
                                                           (constantly "confirmation-uid")
                                                           "sender-uid" "recipient-uid" 10M)
             confirm-transaction-handler (ctf/get-confirm-transaction-form wallet-store confirmation-store)]

         (fact "displays confirm transaction form"
               (let [response
                     (-> (th/create-request :get "/confirm-transaction/confirmation-uid"
                                            {:confirmation-uid "confirmation-uid"}
                                            {:signed-in-uid "sender-uid"})
                         confirm-transaction-handler)]
                 (:status response) => 200
                 (:body response) => (contains #"Confirm transaction")))

         (future-fact "returns 401 when participant not signed in, or does not have apikey")

         (fact "returns 404 when confirmation does not exist"
               (-> (th/create-request :get "/confirm-transaction/nonexisting-confirmation-uid"
                                      {:confirmation-uid "nonexisting-confirmation-uid"}
                                      {:signed-in-uid "sender-uid"})
                   confirm-transaction-handler
                   :status) => 404)

         (fact "returns 404 when confirmation sender-uid does not match signed in user's uid"
               (-> (th/create-request :get "/confirm-transaction/confirmation-uid"
                                      {:confirmation-uid "confirmation-uid"}
                                      {:signed-in-uid "not-the-senders-uid"})
                   confirm-transaction-handler
                   :status) => 404)

         (future-fact "returns 404 when confirmation with provided uid is not a transaction confirmation")))

(facts "about post requests from the confirm transaction form"
       (let [{:keys [blockchain wallet-store sender-wallet sender-apikey recipient-wallet]} (setup-with-sender-and-recipient)
             confirmation-store (fm/create-memory-store)
             form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)]

         (fact "returns 401 when participant not authenticated"
               (-> (th/create-request :post "/post-confirm-transaction-form" {})
                   form-post-handler
                   :status) => 401)

         (fact "returns 401 when participant authenticated but not authorised"
               (-> (th/create-request :post "/post-confirm-transaction-form"
                                      {} {:signed-in-uid "sender-uid"})
                   form-post-handler
                   :status) => 401)

         (fact "returns 401 when transaction was not created by the signed-in participant"
               (let [confirmation-store (fm/create-memory-store)
                     confirmation-for-different-sender (c/new-transaction-confirmation!
                                                        confirmation-store (constantly "confirmation-for-different-sender-uid")
                                                        "different-sender-uid" "recipient-uid" 10M)
                     form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)]
                 (-> (th/create-request :post "/post-confirm-transaction-form"
                                        {:confirmation-uid "confirmation-for-different-sender-uid"}
                                        {:signed-in-uid "sender-uid" :cookie-data sender-apikey})
                     form-post-handler
                     :status)) => 401)

         (facts "when participant is authenticated and authorised, and is attempting to confirm own transaction"
                (let [confirmation-store (fm/create-memory-store)
                      confirmation (c/new-transaction-confirmation!
                                    confirmation-store (constantly "confirmation-uid")
                                    "sender-uid" "recipient-uid" 10M)
                      form-post-handler (ctf/post-confirm-transaction-form wallet-store confirmation-store blockchain)
                      response (-> (th/create-request :post "/post-confirm-transaction-form"
                                                      {:confirmation-uid "confirmation-uid"}
                                                      {:signed-in-uid "sender-uid" :cookie-data sender-apikey})
                                   form-post-handler)]

                  (fact "creates a transaction"
                        (:transaction-count (test-store/summary blockchain)) => 1)

                  (fact "deletes the confirmation"
                        (:entry-count (test-store/summary confirmation-store)) => 0)

                  (fact "redirects to signed-in participant's account page"
                        response => (th/check-redirects-to (absolute-path :account :uid (:uid sender-wallet))))))))

(defn test-activity [from-name amount to-name date]
  {"@context"  "https://www.w3.org/ns/activitystreams"
   "@type"     "Transaction"
   "published" (time/format date)
   "actor"     {"@type"       "Person"
                "displayName" from-name}
   "object"    {"@type"       "INMEMORYBLOCKCHAIN"
                "displayName" (str amount " -> " to-name)}})

(facts "Can load json activitites"
       (let [wallet-store (fm/create-memory-store)
             blockchain (fb/create-in-memory-blockchain :bk)
             _ (blockchain/make-transaction blockchain "ant" 1 "bee" nil)
             _ (blockchain/make-transaction blockchain "cat" 2 "dog" nil)
             _ (blockchain/make-transaction blockchain "eel" 3 "frog" nil)
             activity-handler (tl/list-all-activity-streams wallet-store blockchain)
             d1 (time/add-days (time/datetime 2015 12 1) 1)
             d2 (time/add-days (time/datetime 2015 12 1) 2)
             d3 (time/add-days (time/datetime 2015 12 1) 3)
             expected-ordered-activities [(test-activity "ant" 1 "bee" d1)
                                          (test-activity "cat" 2 "dog" d2)
                                          (test-activity "eel" 3 "frog" d3)]]
         (fact "Content type is activity+json"
               (let [request (rmr/request :get "/activities")
                     response (activity-handler request)]
                 (get-in response [:headers "Content-Type"])) => "application/activity+json;charset=utf-8")
         (fact "Can load all (and are sorted in desc time order)"
               (let [request (rmr/request :get "/activities")]
                 (->> request activity-handler :body cheshire/parse-string) => expected-ordered-activities))
         (fact "Can load using time query"
                 (let [request (rmr/request :get (format "/activities?from=%s&to=%s" d1 d3))]
                   (-> request activity-handler :body cheshire/parse-string) => [(test-activity "cat" 2 "dog" d2)]))

         #_(fact "Can load signed activities using signed query"
                 (let [request (rmr/request :get "/activities?signed=true")
                       response (activity-handler request)
                       external-jwk-set-url "https://external.url/as2/jwk-set"]
                   (fact "the correct content-type is used"
                         (-> response :headers (get "Content-Type")) => "application/jose+json; charset=utf-8")

                   (let [json-response-body (-> response :body cheshire/parse-string)
                         jws-signed-payload (get json-response-body "jws-signed-payload")]
                     (fact "body includes jku which is the external-jwk-set-url (absolute path of jwk-set)"
                           (get json-response-body "jku") => external-jwk-set-url)
                     (fact "body contains the jws-signed-payload"
                           jws-signed-payload =not=> nil?)
                     (fact "jws-signed-payload can be decoded and contains the activities"
                           (-> (jt/verify-signature-and-decode jws-signed-payload jt/test-json-web-key)
                               cheshire/parse-string) => expected-ordered-activities))))))
