(ns freecoin.test.handlers.transactions
  (:require [midje.sweet :refer :all]
            [freecoin.test.test-helper :as th]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.blockchain :as b]
            ;[freecoin.handlers.transactions :as ft]
            ))

;; WIP: DM 20150916
#_(facts "about the transaction form"
       (fact "cannot be accessed when user is not authenticated"
             (let [wallet-store (fm/create-memory-store)
                   transaction-form-handler (ft/get-transaction-form wallet-store)
                   response (-> (th/create-request
                                 :get "/get-transaction-form" {})
                                transaction-form-handler)]
               (:status response) => 401))
       
       (fact "cannot be accessed when user session does not include wallet secret")
       
       (fact "can be accessed by authenticated user with matching wallet secret"
             (let [wallet-store (fm/create-memory-store)
                   wallet (w/new-empty-wallet! wallet-store "sso-id" "name" "test@email.com")
                   blockchain (b/create-in-memory-blockchain :bk)
                   wallet-with-blockchain (w/add-blockchain-to-wallet-with-id! wallet-store blockchain (:uid wallet))
                   
                   ])))

