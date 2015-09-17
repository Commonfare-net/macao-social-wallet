(ns freecoin.test.handlers.transactions
  (:require [midje.sweet :refer :all]
            [freecoin.test.test-helper :as th]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.db.uuid :as uuid]
            [freecoin.blockchain :as fb]
            [freecoin.handlers.transactions :as ft]
            [freecoin.test-helpers.store :as test-store]))

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

(facts "about the transaction form"
       (let [{:keys [wallet-store sender-wallet sender-apikey]} (setup-with-sender-and-recipient)
             transaction-form-handler (ft/get-transaction-form wallet-store)]
         (fact "returns 401 when participant is not authenticated"
               (-> (th/create-request :get "/get-transaction-form" {})
                   transaction-form-handler
                   :status) => 401)
         
         (fact "returns 401 when participant authenticated but without cookie-data"
               (-> (th/create-request :get "/get-transaction-form"
                                      {} {:signed-in-uid "sender-uid"})
                   transaction-form-handler
                   :status) => 401)

         ;; TODO: DM 20150916 - May not be necessary, but perhaps poor
         ;; UX if user can attempt to make a transaction but fail due to
         ;; an invalid cookie-data
         (future-fact "cannot be accessed by authenticated user with invalid cookie-data")
         
         (fact "returns 200 when participant authenticated with cookie-data"
               (let [response (-> (th/create-request :get "/get-transaction-form"
                                                     {} {:signed-in-uid "sender-uid"
                                                         :cookie-data sender-apikey})
                                  transaction-form-handler)]
                 (:status response) => 200
                 (:body response) => (contains #"Make a transaction"))))

       (future-facts "about redisplaying the form after validation issues"))

(facts "about post requests from the transaction form"
       (let [{:keys [wallet-store sender-wallet sender-apikey]} (setup-with-sender-and-recipient)
             confirmation-store ...confirmation-store...
             form-post-handler (ft/post-transaction-form wallet-store confirmation-store)]
         (fact "returns 401 when participant not authenticated"
               (-> (th/create-request :post "/post-transaction-form" {})
                   form-post-handler
                   :status) => 401)

         (fact "returns 401 when participant authenticated but without cookie-data"
               (-> (th/create-request :post "/post-transaction-form"
                                      {} {:signed-in-uid "sender-uid"})
                   form-post-handler
                   :status) => 401)
         
         (facts "when participant is authenticated, has cookie-data, and posts a valid form"
                (let [confirmation-store (fm/create-memory-store)
                      form-post-handler (ft/post-transaction-form wallet-store confirmation-store)
                      response (-> (th/create-request
                                    :post "/post-transaction-form"
                                    {:amount "5.00" :recipient "recipient-uid"}
                                    {:signed-in-uid "sender-uid" :cookie-data sender-apikey})
                                   form-post-handler)]
                  (fact "creates a transaction confirmation"
                        (test-store/entry-count confirmation-store) => 1
                        (:status response) => 201)))

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
                  response => (th/check-redirects-to "/get-transaction-form")))

          ?amount        ?recipient
          "0.0"          "recipient-uid"
          "not-a-float"  "recipient-uid"
          "5.0"          "nonexistent-uid"
          "5.0"          nil
          nil            "recipient-uid")))
