(ns freecoin.journey.transactions
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [clojure.tools.logging :as log] 
            [ring.util.response :as r]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]
            [freecoin.core :as fc]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain}))

(background
  (soc/request-access-token! anything "sender") => {:user-info {:sub "sender"
                                                                :email "sender@email.com"}}
  (soc/request-access-token! anything "recipient") => {:user-info {:sub "recipient"
                                                                   :email "recipient@email.com"}})

(defn sign-up [state auth-code]
  (-> state
      (k/visit (str (routes/absolute-path (c/create-config) :sso-callback) "?code=" auth-code))
      (kc/check-and-follow-redirect "to account page")))

(def sign-in sign-up)

(defn sign-out [state]
  (k/visit state (routes/absolute-path (c/create-config) :sign-out)))

(facts "Participant can send freecoins to another account"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-uid kh/state-on-account-page->uid)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-uid kh/state-on-account-page->uid)

             ;; visit the transactions page and show that there is no transaction
             (k/visit (routes/absolute-path (c/create-config) :get-user-transactions :uid (kh/recall memory :sender-uid)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :uid (kh/recall memory :sender-uid))
             (kc/selector-matches-count ks/transactions-page--table-rows 0)

             ;; do a transaction
             (k/visit (routes/absolute-path (c/create-config) :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10.0")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")
             (kc/check-page-is :account [ks/account-page-body] :uid (kh/recall memory :sender-uid))
             (kc/selector-includes-content [ks/account-page--balance] "-10")

             ;; visit the transactions page and show that there is now one transaction
             (k/visit (routes/absolute-path (c/create-config) :get-user-transactions :uid (kh/recall memory :sender-uid)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :uid (kh/recall memory :sender-uid))
             (kc/selector-matches-count ks/transactions-page--table-rows 1)
             
             (sign-in "recipient")
             (kc/check-page-is :account [ks/account-page-body] :uid (kh/recall memory :recipient-uid))
             (kc/selector-includes-content [ks/account-page--balance] "10")

             ;; visit the transactions page of the recipient and show that there is now one transaction as well
             (k/visit (routes/absolute-path (c/create-config) :get-user-transactions :uid (kh/recall memory :recipient-uid)))
             (kc/check-page-is :get-user-transactions ks/transactions-page-body :uid (kh/recall memory :recipient-uid))
             (kc/selector-matches-count ks/transactions-page--table-rows 1)
             
             )))

(facts "Error messages show in form on invalid input"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-uid kh/state-on-account-page->uid)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-uid kh/state-on-account-page->uid)

             ;; required form fields
             (k/visit (routes/absolute-path (c/create-config) :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "")
             (kc/check-and-fill-in ks/transaction-form--amount "")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "back to form")
             (kc/check-page-is :get-transaction-form [ks/transaction-form--submit])
             (kc/selector-includes-content [ks/transaction-form--error-message] "Required field")

             ;; invalid recipient
             (kc/check-and-fill-in ks/transaction-form--recipient "non-existing-recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "33")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "back to form")
             (kc/check-page-is :get-transaction-form [ks/transaction-form--submit])
             (kc/selector-includes-content [ks/transaction-form--error-message] "Recipient: Not found")

             )))
             
