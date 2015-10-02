(ns freecoin.journey.transactions
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
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
  (k/visit state (str (routes/absolute-path (c/create-config) :sso-callback) "?code=" auth-code)))

(defn sign-out [state]
  (k/visit state (routes/absolute-path (c/create-config) :sign-out))
  ;; TODO: May need to follow redirect to actually
  ;; sign out...
  )

(facts "Participant can send freecoins to another account"
       (-> (k/session test-app)
           (sign-up "recipient")
           sign-out
           (sign-up "sender")
           (k/visit (routes/absolute-path (c/create-config) :get-transaction-form))
           (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
           (kc/check-and-fill-in ks/transaction-form--amount "10.0")
           (kc/check-and-press ks/transaction-form--submit)
           (kc/check-and-follow-redirect "to confirm transaction")
;           (kc/check-and-press ks/confirm-transaction-form--submit)
           ))
