(ns freecoin.journey.helpers
  (:require [taoensso.timbre :as log]
            [freecoin-lib.core :as blockchain] 
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.routes :as routes]
            [freecoin.test-helpers.integration :as ih]
            [kerodon.core :as k]
            [midje.sweet :refer :all]
            [just-auth.db.account :as account]))

(def password "abcd12*!")

;; TODO: at the moment the only way to give an account a flag is by directly accessing the DB. Thats not suitable for the journey tests
(defn make-admin [stores-m email]
  (account/add-flag! (:account-store stores-m) email :admin))

(defn sign-up [state name]
  (let [email (str name "@mail.com")]
    (-> state
        (k/visit (routes/absolute-path :landing-page))
        (kc/selector-exists ks/sign-in-link) 
        (k/follow ks/sign-in-link)
        (kc/check-and-fill-in ks/auth-sign-up-form-first name)
        (kc/check-and-fill-in ks/auth-sign-up-form-last name)
        (kc/check-and-fill-in ks/auth-sign-up-form-email email)
        (kc/check-and-fill-in ks/auth-sign-up-form-pswrd password)
        (kc/check-and-fill-in ks/auth-sign-up-form-conf-pswrd password)
        (kc/check-and-press ks/auth-sign-up-form-submit)
        (kc/check-and-follow-redirect "to activation email sent page")
        (kc/check-page-is :email-confirmation [ks/email-confirmation-body]))))

(defn activate-account
  [state {:keys [activation-id email stores-m]}]
  "At the moment the only way to give an account a flag is by directly accessing the DB."
  (when stores-m
    (make-admin stores-m email))

  (-> state
      (k/visit (routes/absolute-path :activate-account
                                     :activation-id activation-id
                                     :email email))
      (kc/check-and-follow-redirect "redirects to the account activated page")
      (kc/check-page-is :account-activated [ks/account-activated-body])))

(defn sign-in [state name]
  "Manually create the DB entries so we dont have to go through the email sending (that is tested on a separate journey)"
  (let [email (str name "@mail.com")]
(-> state
        (k/visit (routes/absolute-path :sign-in))
        (kc/check-and-fill-in ks/auth-sign-in-form-email email)
        (kc/check-and-fill-in ks/auth-sign-in-form-pswrd password)
        (kc/check-and-press ks/auth-sign-in-form-submit)
        (kc/check-and-follow-redirect "redirects to the user wallet")
        (kc/check-page-is :account [ks/account-page-body] :email email))))

(defn sign-out [state]
  (k/visit state (routes/absolute-path :sign-out)))

(defn log-inline [state & args]
  (log/info (conj args state))
  state)

(defn debug [state]
  (clojure.pprint/pprint state)
  state)

(defn get-activation-id [stores-m email]
  (-> stores-m
      :account-store
      (account/fetch email)
      :activation-link
      just-auth.util/link->token))
