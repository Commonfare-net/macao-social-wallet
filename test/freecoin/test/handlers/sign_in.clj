(ns freecoin.test.handlers.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [stonecutter-oauth.client :as sc]
            [freecoin.config :as config]
            [freecoin.routes :as routes]
            [freecoin.blockchain :as fb]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.handlers.sign-in :as fs]
            [freecoin.test.test-helper :as th]
            [freecoin.test-helpers.store :as test-store]))

(def sso-url "http://SSO_URL")
(def client-id "CLIENT_ID")
(def client-secret "CLIENT_SECRET")
(def callback-uri "CALLBACK_URI")
(def public-key "PUBLICK_KEY") ;; TODO: load this from jwk file

(def absolute-path (partial routes/absolute-path))

(def empty-wallet {})

(def test-sso-config (sc/configure sso-url client-id client-secret callback-uri))

(def db-connection (atom nil))

(def test-email "test@email.com")

(facts "About the landing page"
       (fact "When not signed in, displays link to sign in with stonecutter"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   landing-page-handler (fs/landing-page wallet-store blockchain)
                   response (landing-page-handler (rmr/request :get "/landing-page"))]
               (:status response) => 200
               (-> (:body response) (html/html-snippet [:body])) => (th/links-to? [:.clj--sign-in-link] "/sign-in-with-sso")))

       (fact "When signed in, redirects to the account page"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                        "stonecutter-user-id" "name" test-email))
                   landing-page-handler (fs/landing-page wallet-store blockchain)
                   response (landing-page-handler (-> (rmr/request :get "/landing-page")
                                                      (assoc :session {:signed-in-email (:email wallet)})))]
               response => (th/check-redirects-to (absolute-path :account :email (:email wallet))))))

(fact "the sign-in endpoint redirects to the stonecutter authorisation url"
      (let [sign-in-handler (fs/sign-in test-sso-config)
            response (sign-in-handler (rmr/request :get "/sign-in-with-sso"))
            expected-authorisation-url (str sso-url "/authorisation?"
                                            "client_id=" client-id
                                            "&response_type=code"
                                            "&redirect_uri=" callback-uri)]
        response => (th/check-redirects-to expected-authorisation-url)))

(facts "About the openid callback endpoint"
       (facts "When token request yields a valid access_token + id_token"
              (against-background
               (sc/request-access-token! ...sso-config... ...auth-code...)
               => {:access_token ...access-token...
                   :user-info {:sub "stonecutter-user-id"
                               :email test-email
                               :email_verified true}})
              (fact "if new user, creates a wallet and redirects to account page"
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          callback-handler (fs/sso-callback wallet-store blockchain
                                                            ...sso-config...)
                          response (-> (rmr/request :get "/sso-callback")
                                       (assoc :params {:code ...auth-code...})
                                       callback-handler)]
                      response => (th/check-redirects-to (absolute-path :account :email test-email))
                      response => (th/check-signed-in-as test-email)
                      response => th/check-has-wallet-key
                      (test-store/entry-count wallet-store) => 1))

              (fact "if user exists, signs user in and redirects to index without creating a new wallet"
                    (let [wallet-store (fm/create-memory-store)
                          blockchain (fb/create-in-memory-blockchain :bk)
                          wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                               "stonecutter-user-id" "name" test-email))
                          callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                          response (-> (rmr/request :get "/sso-callback")
                                       (assoc :params {:code ...auth-code...})
                                       callback-handler)]
                      response => (th/check-redirects-to (absolute-path :account :email (:email wallet)))
                      response => (th/check-signed-in-as (:email wallet))
                      response =not=> th/check-has-wallet-key
                      (test-store/entry-count wallet-store) => 1)))

       (fact "When authorisation code is not provided, redirects to landing page"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                   response (-> (rmr/request :get "/sso-callback")
                                callback-handler)]
               response => (th/check-redirects-to (absolute-path :landing-page))))

       (fact "When token response fails, redirects to landing page"
             (against-background
              (sc/request-access-token! ...sso-config... ...invalid-auth-code...) =throws=> (Exception. "Something went wrong"))
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   callback-handler (fs/sso-callback wallet-store blockchain ...sso-config...)
                   response (-> (rmr/request :get "/sso-callback")
                                (assoc :params {:code ...invalid-auth-code...})
                                callback-handler)]
               response => (th/check-redirects-to (absolute-path :landing-page)))))

(fact "signing out redirects to the index with session reset"
      (let [response (-> (th/create-request :get "/sign-out"
                                     nil
                                     {:signed-in-email test-email
                                      :cookie-data "secret"})
                         fs/sign-out)]
        response => (th/check-redirects-to (absolute-path :index))
        response =not=> (th/check-signed-in-as test-email)))
