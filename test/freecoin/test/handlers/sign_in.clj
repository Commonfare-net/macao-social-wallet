;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2017 Dyne.org foundation

;; Sourcecode designed, written and maintained by
;; Aspasia Beneti <aspra@dyne.org>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU Affero General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU Affero General Public License for more details.

;; You should have received a copy of the GNU Affero General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns freecoin.test.handlers.sign-in
  (:require [midje.sweet :refer :all]
            [net.cgrand.enlive-html :as html]
            [ring.mock.request :as rmr]
            [freecoin.config :as config]
            [freecoin.routes :as routes]
            [freecoin.blockchain :as fb]
            [freecoin.db.mongo :as fm]
            [freecoin.db.wallet :as w]
            [freecoin.handlers.sign-in :as handler]
            [freecoin.test.test-helper :as th]
            [freecoin.test-helpers.store :as test-store]
            [freecoin.email-activation :as email-activation]
            [taoensso.timbre :as log]))

(def client-id "CLIENT_ID")
(def client-secret "CLIENT_SECRET")
(def callback-uri "CALLBACK_URI")
(def public-key "PUBLICK_KEY") ;; TODO: load this from jwk file

(def absolute-path (partial routes/absolute-path))

(def empty-wallet {})

(def db-connection (atom nil))

(def test-email "test@email.com")

(facts "About the landing page"
       (fact "When not signed in, displays link to sign in"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   landing-page-handler (handler/landing-page wallet-store)
                   response (landing-page-handler (rmr/request :get "/landing-page"))]
               (:status response) => 200
               (-> (:body response) (html/html-snippet [:body])) => (th/links-to? [:.clj--sign-in-link] "/sign-in")))

       (fact "When signed in and activated redirects to the account page"
             (let [wallet-store (fm/create-memory-store)
                   blockchain (fb/create-in-memory-blockchain :bk)
                   wallet (:wallet (w/new-empty-wallet! wallet-store blockchain 
                                                        "name" test-email))
                   landing-page-handler (handler/landing-page wallet-store)
                   response (landing-page-handler (-> (rmr/request :get "/landing-page")
                                                      (assoc :session {:signed-in-email (:email wallet)})))]
               response => (th/check-redirects-to (absolute-path :account :email (:email wallet))))))

(facts "Sign up"
       (let [user-email "user@mail.com"
             password "12345678"
             emails (atom [])
             account-store (fm/create-memory-store)
             wallet-store (fm/create-memory-store)
             email-activator (email-activation/->StubActivationEmail emails account-store)
             create-account-handler (handler/create-account account-store email-activator)
             blockchain (fb/create-in-memory-blockchain :bk)
             sign-in-handler (handler/log-in account-store wallet-store blockchain)]
         (fact "if new user, send email and redirect to activation page"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-up-form)
                                                     {:first-name "first-name"
                                                      :last-name "last-name"
                                                      :email user-email
                                                      :password password
                                                      :confirm-password password})
                                  create-account-handler)
                     activation-url (-> @emails (first) :activation-url)
                     user-account (fm/fetch account-store user-email)]
                 response => (th/check-redirects-to (absolute-path :email-confirmation))
                 (-> @emails (first) :email) => user-email
                 (.contains activation-url (:activation-id user-account)) => truthy
                 (.contains activation-url (:email user-account)) => truthy
                 (:activated (fm/fetch account-store user-email)) => false))
         
         (fact "Cannot sign in before the account is activated"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-in-form)
                                                     {:sign-in-email user-email
                                                      :sign-in-password password})
                                  sign-in-handler)]
                 response => (th/check-redirects-to (absolute-path :sign-in-form))
                 (-> response :flash (first) :msg) => "The account for user@mail.com is not yet active."))
         
         (fact "When activation link is clicked the account is activated"
               (let [activation-id (:activation-id (fm/fetch account-store user-email))
                     activation-handler (handler/activate-account account-store)
                     response (-> (rmr/request :get "/activate/")
                                  (assoc :params {:email user-email :activation-id activation-id})
                                  activation-handler)]
                 response => (th/check-redirects-to (absolute-path :account-activated))))

         (fact "Can now sign in and redirected to the wallet page"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-in-form)
                                                     {:sign-in-email user-email
                                                      :sign-in-password password})
                                  sign-in-handler)]
                 response => (th/check-redirects-to (absolute-path :account :email user-email))
                 response => (th/check-signed-in-as user-email)
                 (test-store/entry-count wallet-store) => 1))

         (fact "If trying to create an account with an existing email an error is returned"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-up-form)
                                                     {:first-name "first-name"
                                                      :last-name "last-name"
                                                      :email user-email
                                                      :password password
                                                      :confirm-password password})
                                  create-account-handler)]
                 (-> response :flash (first) :msg) => (str "An account with email " user-email " already exists.")))

         (fact "If a field is missing during sign up an error is returned"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-up-form)
                                                     {:first-name "first-name"
                                                      :email "another@mail.com"
                                                      :password password
                                                      :confirm-password password})
                                  create-account-handler)]
                 (-> response :flash (first) :keys (first)) => :last-name
                 (-> response :flash (first) :msg) => "must not be blank"))
         
         (fact "If password and confirmation password are not the same an error is returned"
               (let [response (-> (th/create-request :post
                                                     (absolute-path :sign-up-form)
                                                     {:first-name "first-name"
                                                      :last-name "last-name"
                                                      :email "yet-another@mail.com"
                                                      :password password
                                                      :confirm-password "another-password"})
                                  create-account-handler)]
                 (-> response :flash (first) :msg) => "The conformation password has to be the same as the password"))))

(fact "signing out redirects to the index with session reset"
      (let [response (-> (th/create-request :get "/sign-out"
                                            nil
                                            {:signed-in-email test-email
                                             :cookie-data "secret"})
                         handler/sign-out)]
        response => (th/check-redirects-to (absolute-path :index))
        response =not=> (th/check-signed-in-as test-email)))
