(ns freecoin.test.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [stonecutter-oauth.client :as sc]
            [freecoin.test.test-helper :as th]
            [freecoin.handlers.sign-in :as fs]))


(def sso-url "SSO_URL")
(def client-id "CLIENT_ID")
(def client-secret "CLIENT_SECRET")
(def callback-uri "CALLBACK_URI")
(def public-key "PUBLICK_KEY") ;; TODO: load this from jwk file

(def test-sso-config (sc/configure sso-url client-id client-secret callback-uri :protocol :openid :public-key public-key))

(facts "About signing up via a Stonecutter SSO instance"
       (facts "About the landing page"
              (fact "When not signed in, displays link to sign in with stonecutter"
                    (let [sign-in-handler (fs/landing-page test-sso-config)
                          response (sign-in-handler (rmr/request :get "/"))]
                      (:status response) => 200
                      (-> (:body response) (html/html-snippet [:body])) => (th/links-to? [:.clj--sign-in-link] sso-url)))
              
              (fact "When signed in, displays user's balance"))
       
       (facts "About the openid callback endpoint"
              (facts "When able to receive a valid access_token + id_token"
                     (fact "if new user, creates a wallet and redirects to landing page")
                     (fact "if existing user, retrieves wallet, and redirects to landing page"))
              
              (fact "Redirects to landing page (?) when not accessed as part of a successful openid authentication flow")))
