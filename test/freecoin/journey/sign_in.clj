(ns freecoin.journey.sign-in
  (:require [midje.sweet :refer :all]
            [kerodon.core :as k]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.test-helpers.integration :as ih]
            [just-auth.core :as auth]
            [freecoin-lib.core :as blockchain]
            [freecoin.routes :as routes]
            [freecoin-lib.config :as c]
            [taoensso.timbre :as log]
            [freecoin-lib.db.freecoin :as db]
            [just-auth.db 
             [account :as account]
             [password-recovery :as pass]
             [just-auth :as auth-db]]))

(ih/setup-db)

(fact "setup-db is not null" ih/get-test-db => truthy)

(def freecoin-stores (db/create-freecoin-stores (ih/get-test-db) {}))
;; TTL is set to 30 seconds but mongo checks only every ~60 secs
(def stores-m (merge freecoin-stores
                     (auth-db/create-auth-stores (ih/get-test-db) {:ttl-password-recovery 30})))
(def blockchain (blockchain/new-mongo freecoin-stores))
(def emails (atom []))

(def test-app (ih/build-app {:stores-m stores-m 
                             :blockchain blockchain
                             :email-authenticator (auth/new-stub-email-based-authentication 
                                                   stores-m
                                                   emails)}))

(def ^:dynamic email "id-1@email.com")
(def password "abcd12*!")

(defn latest-email [] (last @emails))

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
             (:email (latest-email)) => email)

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
             (let [activation-url (-> @emails (first) :activation-link)
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

(facts "Can resend an email invitation"
       (fact "Can resend an email invitation for an existing account"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :sign-in))
                 (kc/check-and-fill-in ks/auth-resend-form-email email)
                 (kc/check-and-press ks/auth-resend-form-submit)
                 (kc/check-and-follow-redirect "redirects to the email sent page")
                 (kc/check-page-is :email-confirmation [ks/email-confirmation-body])))

       (fact "Email was sent"
             (-> @emails (count)) => 2
             (:email (latest-email)) => email)
       
       (fact "If an account with the email does not exist no email can be sent"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :sign-in))
                 (kc/check-and-fill-in ks/auth-resend-form-email "some-uknown@mail.com")
                 (kc/check-and-press ks/auth-resend-form-submit)
                 (kc/check-and-follow-redirect "redirects to same page (sign-in) with an error")
                 (kc/check-page-is :sign-in [ks/auth-form-problems]))))

;; FIXME: workaround due to midje bug see https://github.com/marick/Midje/issues/275. With facts it wouldn't work and if not all nested facts wouldn't have the same metadata it wouldn't work either (see bellow)
(fact-group "User forgot password and tries to reset it" :slow
       (fact "The user requests password reset"
             (-> (k/session test-app)
                 (k/visit (routes/absolute-path :sign-in))
                 (kc/check-and-fill-in ks/auth-password-recovery-email email)
                 (kc/check-and-press ks/auth-password-recovery-submit)
                 (kc/check-and-follow-redirect "redirects to the email sent page")
                 (kc/check-page-is :email-confirmation [ks/email-confirmation-body])))

       (fact "Email was sent"
             (-> @emails (count)) => 3
             (:email (latest-email)) => email)

       (fact "check that the password recovery entry has been created"
             (pass/fetch (:password-recovery-store stores-m) email) => truthy)

       (fact "Change password using link"
             (let [old-password-hash (:password (account/fetch (:account-store stores-m) email))
                   password-recovery-url (:password-recovery-link (latest-email))
                   password-recovery-id (-> password-recovery-url (clojure.string/split #"/") (last))]
               (-> (k/session test-app)
                   (k/visit (routes/absolute-path :reset-password
                                                  :password-recovery-id password-recovery-id
                                                  :email email))
                   (kc/check-and-fill-in ks/change-password-new "new-password")
                   (kc/check-and-fill-in ks/change-password-repeat "new-password")
                   (kc/check-and-press ks/change-password-submit)
                   (kc/check-and-follow-redirect "redirects to password changed page")
                   (kc/check-page-is :password-changed [ks/password-changed-body]))

               (fact "check that the password has changed"
                     (= old-password-hash (:password (account/fetch (:account-store stores-m) email))) => falsey)

               (fact "check that the password recovery entry has been deleted"
                     (pass/fetch (:password-recovery-store stores-m) email) => falsey)))

       ;; FIXME: workaround due to midje bug see https://github.com/marick/Midje/issues/275. With facts it wouldn't work and if not all nested facts wouldn't have the same metadata it wouldn't work either (see above)
       (fact "Check that the link cannot be used after expired" :slow
             (fact "Request another password recovery link and check that it gets deleted from the DB automatically after 30 seconds" :slow
                   (-> (k/session test-app)
                       (k/visit (routes/absolute-path :sign-in))
                       (kc/check-and-fill-in ks/auth-password-recovery-email email)
                       (kc/check-and-press ks/auth-password-recovery-submit)
                       (kc/check-and-follow-redirect "redirects to the email sent page")
                       (kc/check-page-is :email-confirmation [ks/email-confirmation-body]))

                   (pass/fetch (:password-recovery-store stores-m) email) => truthy
                   (clojure.pprint/pprint "I am testing the DB expiration, this will last about a minute, please bear with me...")
                   (Thread/sleep 90000)

                   (pass/fetch (:password-recovery-store stores-m) email) => falsey)))

(facts "Participant can sign out"
       (-> (k/session test-app) 
           (k/visit (routes/absolute-path :sign-out))
           (kc/check-and-follow-redirect "to index page")
           (kc/check-page-is :index [ks/index-page-body])
           ;; TODO: DM 20150922 - Assertion that participant has in
           ;; fact signed out.
           ))



