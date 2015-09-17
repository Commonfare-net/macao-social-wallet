(ns freecoin.test.handlers.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [stonecutter-oauth.client :as sc]
            [freecoin.storage :as storage]
            [freecoin.blockchain :as fb]
            [freecoin.db.mongo :as fm]
            [freecoin.db.uuid :as uuid]
            [freecoin.db.wallet :as w]
            [freecoin.handlers.sign-in :as fs]
            [freecoin.integration.storage-helpers :as sh]
            [freecoin.test.test-helper :as th]
            [freecoin.test-helpers.store :as test-store])
  (:import [freecoin.db.mongo MemoryStore]))

(def sso-url "http://SSO_URL")
(def client-id "CLIENT_ID")
(def client-secret "CLIENT_SECRET")
(def callback-uri "CALLBACK_URI")
(def public-key "PUBLICK_KEY") ;; TODO: load this from jwk file

(def empty-wallet {})

(def test-sso-config (sc/configure sso-url client-id client-secret callback-uri))

(def db-connection (atom nil))

(facts "About signing up via a Stonecutter SSO instance"
       (facts "About the landing page"
              (fact "When not signed in, displays link to sign in with stonecutter"
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          landing-page-handler (fs/landing-page wallet-store blockchain)
                          response (landing-page-handler (rmr/request :get "/"))]
                      (:status response) => 200
                      (-> (:body response) (html/html-snippet [:body])) => (th/links-to? [:.clj--sign-in-link] "/sign-in-with-sso")))
              
              (fact "When signed in, displays users balance"
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          wallet (:wallet (w/new-empty-wallet! wallet-store blockchain uuid/uuid
                                                               "stonecutter-user-id" "name" "test@email.com"))
                          landing-page-handler (fs/landing-page wallet-store blockchain)
                          response (landing-page-handler (-> (rmr/request :get "/")
                                                             (assoc :session {:signed-in-uid (:uid wallet)})))]
                      (:body response) => (contains #"Balance:\s*0"))))

       (fact "the sign-in endpoint redirects to the stonecutter authorisation url"
             (let [sign-in-handler (fs/sign-in test-sso-config)
                   response (sign-in-handler (rmr/request :get "/sign-in-with-sso"))
                   expected-authorisation-url (str sso-url "/authorisation?"
                                                   "client_id=" client-id
                                                   "&response_type=code"
                                                   "&redirect_uri=" callback-uri)]
               response => (th/check-redirects-to expected-authorisation-url)))

       (facts "About the openid callback endpoint"
              (against-background
               [(before :facts (do (reset! db-connection (storage/connect sh/test-db-config))
                                   (sh/clear-db @db-connection)))
                (after :facts (do (storage/disconnect @db-connection)))])
              
              (facts "When token request yields a valid access_token + id_token"
                     (against-background
                      (sc/request-access-token! ...sso-config... ...auth-code...) => {:access_token ...access-token...
                                                                                      :user-info {:sub "stonecutter-user-id"
                                                                                                  :email "test@email.com"
                                                                                                  :email_verified true}})
                     (fact "if new user, creates a wallet and redirects to landing page"
                           (against-background (uuid/uuid) => "a-uuid")
                           (let [wallet-store (fm/create-memory-store)
                                 blockchain (fb/create-in-memory-blockchain :bk)
                                 callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                                 response (callback-handler (-> (rmr/request :get "/sso-callback")
                                                                (assoc :params {:code ...auth-code...})))]
                             response => (th/check-redirects-to "/landing-page")
                             response => (th/check-signed-in-as "a-uuid")
                             response => th/check-has-wallet-key
                             (test-store/entry-count wallet-store) => 1))
                     
                     (fact "if user exists, signs user in and redirects to landing page without creating a new wallet"
                           (let [wallet-store (fm/create-memory-store)
                                 blockchain (fb/create-in-memory-blockchain :bk)
                                 wallet (:wallet (w/new-empty-wallet! wallet-store blockchain uuid/uuid
                                                                      "stonecutter-user-id" "name" "test@email.com"))
                                 callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                                 response (callback-handler (-> (rmr/request :get "/sso-callback")
                                                                (assoc :params {:code ...auth-code...})))]
                             response => (th/check-redirects-to "/landing-page")
                             response => (th/check-signed-in-as (:uid wallet))
                             response =not=> th/check-has-wallet-key
                             (test-store/entry-count wallet-store) => 1)))
              
              (fact "When authorisation code is not provided, redirects to landing page"
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                          response (callback-handler (rmr/request :get "/sso-callback"))]
                      response => (th/check-redirects-to "/landing-page")))
              
              (fact "When token response fails, redirects to landing page"
                    (against-background
                     (sc/request-access-token! ...sso-config... ...invalid-auth-code...) =throws=> (Exception. "Something went wrong"))
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                          response (callback-handler (-> (rmr/request :get "/sso-callback")
                                                         (assoc :params {:code ...invalid-auth-code...})))]
                      response => (th/check-redirects-to "/landing-page")))))
