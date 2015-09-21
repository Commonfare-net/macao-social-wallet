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

(def ^:dynamic sso-id "sso-id-1")
(def ^:dynamic email "id-1@email.com")

(background
 (soc/request-access-token! anything anything) => {:user-info {:sub sso-id
                                                               :email email}})

(defn sign-in [state]
  (k/visit state (str (routes/absolute-path (c/create-config) :sso-callback) "?code=auth-code")))

(facts "Participant can send freecoins to another account"
       (-> (k/session test-app)
           sign-in
           (k/visit (routes/absolute-path (c/create-config) :get-transaction-form))
           (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
           (kc/check-and-fill-in ks/transaction-form--amount "10.0")
           (kc/check-and-press ks/transaction-form--submit)
           ;; TODO: DM 20150921 - Need to create recipient account
           ;; before attempting transaction.
           ))
