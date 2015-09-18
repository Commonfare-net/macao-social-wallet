(ns freecoin.journey.sign-in
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.util.response :as r]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]
            [freecoin.core :as fc]))

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(def test-app (ih/build-app {;:stores-m stores-m
                             ;:blockchain blockchain
                             }))

(def ^:dynamic sso-id "sso-id-1")
(def ^:dynamic email "id-1@email.com")

(defn debug [state]
  (clojure.pprint/pprint state)
  state)

(background
 (soc/request-access-token! anything anything) => {:user-info {:sub sso-id
                                                               :email email}})

(facts "User can access landing page"
       (-> (k/session test-app)
           (k/visit (routes/absolute-path (c/create-config) :landing-page))
           (kc/check-page-is :landing-page [ks/landing-page-body])))

(facts "A participant can authenticate and create an account, then is redirected to the index to view their balance"
       (against-background
        (soc/authorisation-redirect-response anything)
        => (r/redirect (str (routes/absolute-path (c/create-config) :sso-callback)
                            "?code=auth-code")))
       (-> (k/session test-app)
           (k/visit (routes/absolute-path (c/create-config) :sign-in))
           (kc/check-and-follow-redirect "to stonecutter callback")
           (kc/check-and-follow-redirect "to landing-page")
           (kc/check-page-is :index [ks/index-page-body])))
