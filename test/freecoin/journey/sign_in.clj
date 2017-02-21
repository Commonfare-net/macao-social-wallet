(ns freecoin.journey.sign-in
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [ring.util.response :as r]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.db.storage :as s]
            [freecoin.db.uuid :as uuid]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain}))

(def ^:dynamic sso-id "sso-id-1")
(def ^:dynamic email "id-1@email.com")

(defn debug [state]
  (clojure.pprint/pprint state)
  state)

(background
 (soc/request-access-token! anything anything) => {:user-info {:sub sso-id
                                                               :email email}})

(defn sign-in [state]
  (k/visit state (routes/absolute-path :sso-callback)))

(facts "User can access landing page"
       (-> (k/session test-app)
           (k/visit (routes/absolute-path :landing-page))
           (kc/check-page-is :landing-page [ks/landing-page-body])))

(facts "A participant can authenticate and create an account, then is redirected to the account page to view their balance"
       (against-background
        (soc/authorisation-redirect-response anything)
        => (r/redirect (str (routes/absolute-path :sso-callback)
                            "?code=auth-code"))
        (uuid/uuid) => "some-uuid")
       (-> (k/session test-app)
           (k/visit (routes/absolute-path :sign-in))
           (kc/check-and-follow-redirect "to stonecutter callback")
           (kc/check-and-follow-redirect "to account page")
           (kc/check-page-is :account [ks/account-page-body] :email email)))

(facts "Participant can sign out"
       (-> (k/session test-app)
           sign-in
           (k/visit (routes/absolute-path :sign-out))
           (kc/check-and-follow-redirect "to index page")
           (kc/check-page-is :index [ks/index-page-body])
           ; TODO: DM 20150922 - Assertion that participant has in
           ; fact signed out.
           ))
