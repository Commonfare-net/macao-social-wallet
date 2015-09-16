(ns freecoin.test.handlers.transactions
  (:require [midje.sweet :refer :all]
            [freecoin.test.test-helper :as th]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.blockchain :as fb]
            [freecoin.handlers.transactions :as ft]
            ))

(defn setup-with-single-participant []
  (let [wallet-store (fm/create-memory-store)
        blockchain (fb/create-in-memory-blockchain :bk)
        {:keys [wallet apikey]} (w/new-empty-wallet! wallet-store blockchain
                                                     "sso-id" "name" "test@email.com")]
    {:wallet-store wallet-store
     :blockchain blockchain
     :wallet wallet
     :apikey apikey}))

(facts "about the transaction form"
       (let [{:keys [wallet-store wallet apikey]} (setup-with-single-participant)
             transaction-form-handler (ft/get-transaction-form wallet-store)]
         (fact "returns 401 when participant is not authenticated"
               (-> (th/create-request :get "/get-transaction-form" {})
                   transaction-form-handler
                   :status) => 401)
         
         (fact "returns 401 when participant authenticated but without cookie-data"
               (-> (th/create-request :get "/get-transaction-form"
                                      {} {:signed-in-uid (:uid wallet)})
                   transaction-form-handler
                   :status ) => 401)

         ;; TODO: DM 20150916 - May not be necessary, but perhaps poor
         ;; UX if user can attempt to make a transaction but fail due to
         ;; an invalid cookie-data
         (future-fact "cannot be accessed by authenticated user with invalid cookie-data")
         
         (fact "returns 200 when participant authenticated with cookie-data"
               (let [response (-> (th/create-request :get "/get-transaction-form"
                                                     {} {:signed-in-uid (:uid wallet)
                                                         :cookie-data apikey})
                                  transaction-form-handler)]
                 (:status response) => 200
                 (:body response) => (contains #"Make a transaction"))))

       (future-facts "about redisplaying the form after validation issues"))

(facts "about post requests from the transaction form"
       (let [{:keys [wallet-store wallet apikey]} (setup-with-single-participant)
             form-post-handler (ft/post-transaction-form wallet-store)]
         (fact "returns 401 when participant not authenticated"
               (-> (th/create-request :post "/post-transaction-form" {})
                   form-post-handler
                   :status) => 401)

         (fact "returns 401 when participant authenticated but without cookie-data"
               (-> (th/create-request :post "/post-transaction-form"
                                      {} {:signed-in-uid (:uid wallet)})
                   form-post-handler
                   :status) => 401)
         
         (fact "returns 201 when participant authenticated with cookie-data"
               (-> (th/create-request :post "/post-transaction-form"
                                      {} {:signed-in-uid (:uid wallet)
                                          :cookie-data apikey})
                   form-post-handler
                   :status) => 201)))
