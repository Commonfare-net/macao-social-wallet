(ns freecoin.journey.sign-in
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [stonecutter-oauth.client :as soc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.test-helpers.integration :as ih]
            [freecoin.email-activation :as email-activation]
            [freecoin.db.storage :as s]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as c]
            [taoensso.timbre :as log]
            [freecoin.db.account :as account]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub stores-m))
(def emails (atom []))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain
                             :email-activator (email-activation/->StubActivationEmail
                                               emails
                                               (:account-store stores-m))}))

(def ^:dynamic email "id-1@email.com")
(def password "12345678")

(facts "User can access landing page"
       (-> (k/session test-app)
           (k/visit (routes/absolute-path :landing-page))
           (kc/check-page-is :landing-page [ks/landing-page-body])))

(facts "Sign up and sign in"
       (fact "After signing up we redirect to the email confirmation page"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :landing-page))
                 (kc/selector-exists ks/sign-in-link) 
                 (k/follow ks/sign-in-link)
                 (kc/check-and-fill-in ks/auth-sign-up-form-first "first-name")
                 (kc/check-and-fill-in ks/auth-sign-up-form-last "last-name")
                 (kc/check-and-fill-in ks/auth-sign-up-form-email email)
                 (kc/check-and-fill-in ks/auth-sign-up-form-pswrd password)
                 (kc/check-and-fill-in ks/auth-sign-up-form-conf-pswrd password)
                 (kc/check-and-press ks/auth-sign-up-form-submit)
                 (kc/check-and-follow-redirect "to activation email sent page")
                 (kc/check-page-is :email-confirmation [ks/email-confirmation-body])))

       (fact "Email was sent"
             (-> @emails (count)) => 1
             (-> @emails (first) :email) => email)

       (fact "Check that the account is not yet activated"
             (:activated (account/fetch (:account-store stores-m) email)) => false)

       (fact "Check that sign in doesn't work until the account gets activated"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :sign-in))
                 (kc/check-and-fill-in ks/auth-sign-in-form-email email)
                 (kc/check-and-fill-in ks/auth-sign-in-form-email password)
                 (kc/check-and-press ks/auth-sign-in-form-submit)
                 (kc/check-and-follow-redirect "redirects to sign-in page with error")
                 (kc/check-page-is :sign-in [ks/auth-form-problems])))

       (fact "Activate account using link"
             (let [activation-url (-> @emails (first) :activation-url)
                   activation-id (-> activation-url (clojure.string/split #"/") (last))]
               (-> (k/session test-app)
                   (k/visit (routes/absolute-path :activate-account
                                                  :activation-id activation-id
                                                  :email email))
                   (kc/check-and-follow-redirect "redirects to the account activated page")
                   (kc/check-page-is :account-activated [ks/account-activated-body]))))

       (fact "Check that now the account is active"
             (:activated (account/fetch (:account-store stores-m) email)) => true)

       (fact "Check that now the user can sign in and view the wallet"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :sign-in))
                 (kc/check-and-fill-in ks/auth-sign-in-form-email email)
                 (kc/check-and-fill-in ks/auth-sign-in-form-pswrd password)
                 (kc/check-and-press ks/auth-sign-in-form-submit)
                 (kc/check-and-follow-redirect "redirects to the user wallet")
                 (kc/check-page-is :account [ks/account-page-body] :email email)))

       (fact "Check that if we try to create an account with an existing email we get an error and no account is created on the DB"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :landing-page))
                 (kc/selector-exists ks/sign-in-link) 
                 (k/follow ks/sign-in-link)
                 (kc/check-and-fill-in ks/auth-sign-up-form-first "another-first-name")
                 (kc/check-and-fill-in ks/auth-sign-up-form-last "another-last-name")
                 (kc/check-and-fill-in ks/auth-sign-up-form-email email)
                 (kc/check-and-fill-in ks/auth-sign-up-form-pswrd password)
                 (kc/check-and-fill-in ks/auth-sign-up-form-conf-pswrd password)
                 (kc/check-and-press ks/auth-sign-up-form-submit)
                 (kc/check-and-follow-redirect "redirects to same page (sign-in) with an error")
                 (kc/check-page-is :sign-in [ks/auth-form-problems]))))


(facts "Participant can sign out"
       (-> (k/session test-app) 
           (k/visit (routes/absolute-path :sign-out))
           (kc/check-and-follow-redirect "to index page")
           (kc/check-page-is :index [ks/index-page-body])
           ;; TODO: DM 20150922 - Assertion that participant has in
           ;; fact signed out.
           ))



