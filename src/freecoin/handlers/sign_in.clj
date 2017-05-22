;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Aspasia Beneti <aspra@dyne.org>

;; With contributions by
;; Duncan Mortimer <dmortime@thoughtworks.com>

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

(ns freecoin.handlers.sign-in
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [clojure.string :as s]
            [freecoin.routes :as routes]
            [freecoin.db.wallet :as wallet]
            [freecoin.db.account :as account]
            [freecoin.db.mongo :as mongo]
            [freecoin.auth :as auth]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]
            [freecoin.views.index-page :as index-page]
            [freecoin.views.sign-in :as sign-in-page]
            [freecoin.views.email-confirmation :as email-confirmation]
            [freecoin.views.account-activated :as aa]
            [freecoin.form_helpers :as fh]
            [freecoin.context-helpers :as ch]
            [freecoin.email-activation :as email-activation]
            [taoensso.timbre :as log]))

(defn error-redirect [ctx error-message]
  (-> ctx (assoc :error error-message)
      (routes/absolute-path :landing-page)
      r/redirect
      lr/ring-response))

(lc/defresource index-page
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> (index-page/build)
                 fv/render-page))

(lc/defresource landing-page [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :exists? (fn [ctx]
             (if-let [email (:email (auth/is-signed-in ctx))]
               (let [wallet (wallet/fetch wallet-store email)]
                 {:wallet wallet})
               {}))

  :handle-ok (fn [ctx]
               (if-let [wallet (:wallet ctx)]
                 (-> (routes/absolute-path :account :email (:email wallet))
                     r/redirect
                     lr/ring-response)
                 (-> {:sign-in-url "/sign-in"}
                     landing-page/landing-page
                     fv/render-page))))

(lc/defresource sign-in 
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx] 
               (-> ctx 
                   sign-in-page/build
                   fv/render-page)))

(lc/defresource log-in [account-store wallet-store blockchain]
  :allowed-methods [:post]
  :available-media-types ["text/html"]

  :authorized? (fn [ctx]
                 (let [{:keys [status data problems]}
                       (fh/validate-form sign-in-page/sign-in-form
                                         (ch/context->params ctx))]
                   (if (= :ok status)
                     (let [email (-> ctx :request :params :sign-in-email)]
                       (if-let [account (account/fetch account-store email)]
                         (if (:activated account)
                           (if (account/correct-password? account-store email (-> ctx :request :params :sign-in-password))
                             {:email email}
                             [false  (fh/form-problem (conj problems
                                                            {:keys [:sign-in-password] :msg (str "Wrong password for account " email)}))])
                           [false  (fh/form-problem (conj problems
                                                          {:keys [:sign-in-email] :msg (str "The account for " email
                                                                                               " is not yet active.")}))])
                         [false (fh/form-problem (conj problems
                                                        {:keys [:sign-in-email] :msg "Account with for this email does not exist"}))]))
                     (do 
                       [false (fh/form-problem problems)]))))

  :handle-unauthorized (fn [ctx]
                         (lr/ring-response (fh/flash-form-problem
                                            (r/redirect (routes/absolute-path :sign-in-form))
                                            ctx)))

  :post! (fn [ctx]
           ;; the wallet exists already
           (let [email (:email ctx)
                 name (first (s/split email #"@"))]
             (if-let [wallet (wallet/fetch wallet-store email)]
               (do
                 (log/trace "The wallet for email " email " already exists")
                 {::email (:email wallet)})
               
               ;; a new wallet has to be made
               (when-let [{:keys [wallet apikey]}
                          (wallet/new-empty-wallet!
                              wallet-store
                            blockchain 
                            name email)]

                 ;; TODO: distribute other shares to organization and auditor
                 ;; see in freecoin.db.wallet
                 ;; {:wallet (mongo/store! wallet-store :uid wallet)
                 ;;  :apikey       (secret->apikey              account-secret)
                 ;;  :participant  (secret->participant-shares  account-secret)
                 ;;  :organization (secret->organization-shares account-secret)
                 ;;  :auditor      (secret->auditor-shares      account-secret)
                 ;;  }))

                 ;; saved in context
                 {::email (:email wallet)
                  ::cookie-data apikey}))))

  :handle-created (fn [ctx]
                   (lr/ring-response
                    (cond-> (r/redirect (routes/absolute-path :account :email (::email ctx)))
                      (::cookie-data ctx) (assoc-in [:session :cookie-data] (::cookie-data ctx))
                      true (assoc-in [:session :signed-in-email] (::email ctx))))))


(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

(def content-types ["text/html" "application/x-www-form-urlencoded"])

(lc/defresource create-account [account-store email-activator]
  :allowed-methods [:post]
  :available-media-types content-types

  :known-content-type? #(check-content-type % content-types)

  :processable? (fn [ctx]
                  (let [{:keys [status data problems]}
                        (fh/validate-form sign-in-page/sign-up-form
                                          (ch/context->params ctx))]
                    (if (= :ok status)
                      (let [email (-> ctx :request :params :email)]
                        (if (account/fetch account-store email)
                          [false (fh/form-problem (conj problems
                                                        {:keys [:email] :msg (str "An account with email " email
                                                                                     " already exists.")}))]
                          ctx))
                      [false (fh/form-problem problems)])))

  :handle-unprocessable-entity (fn [ctx] 
                                 (lr/ring-response (fh/flash-form-problem
                                                    (r/redirect (routes/absolute-path :sign-in))
                                                    ctx)))
  :post! (fn [ctx]
           (let [data (-> ctx :request :params)
                 email (get data :email)]
             (if (account/new-account! account-store (select-keys data [:first-name :last-name :email :password]))
               (when-not (email-activation/email-activation! email-activator email) 
                 (error-redirect ctx "The activation email failed to send "))
               (log/error "Something went wrong when creating a user in the DB"))))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           :location (routes/absolute-path :email-confirmation))))

(lc/defresource email-confirmation
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (-> ctx
                   (email-confirmation/build)
                   fv/render-page)))

(lc/defresource activate-account [account-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (let [activation-code (get-in ctx [:request :params :activation-id])
                     email (get-in ctx [:request :params :email])]
                 (if-let [account (account/fetch-by-activation-id account-store activation-code)]
                   (if (= (:email account) email)
                     (do (account/activate! account-store (:email account))
                         (-> (routes/absolute-path :account-activated)
                             r/redirect 
                             lr/ring-response))
                     (error-redirect ctx "The email and activation id do not match"))
                   (error-redirect ctx "The activation id could not be found")))))

(lc/defresource resend-activation-email [account-store email-activator]
  :allowed-methods [:post]
  :available-media-types content-types
  :known-content-type? #(check-content-type % content-types)
  :processable? (fn [ctx]
                  (let [{:keys [status data problems]}
                        (fh/validate-form sign-in-page/resend-activation-form
                                          (ch/context->params ctx))]
                    (if (= :ok status)
                      (let [email (-> ctx :request :params :activation-email)]
                        (if-not (account/fetch account-store email)
                          [false (fh/form-problem (conj problems
                                                        {:keys [:email] :msg (str "The email " email " is not registered yet. Please sign up first")}))]
                          ctx))
                      [false (fh/form-problem problems)])))

  :handle-unprocessable-entity (fn [ctx] 
                                 (lr/ring-response (fh/flash-form-problem
                                                    (r/redirect (routes/absolute-path :sign-in))
                                                    ctx)))
  :post! (fn [ctx]
           (let [email (get-in ctx [:request :params :activation-email])
                 account (account/fetch account-store email)]
             (when-not (email-activation/email-activation! email-activator email)
               (error-redirect ctx "The activation email failed to send")
               (log/error "The activation email failed to send"))))

  :post-redirect? (fn [ctx] 
                    (assoc ctx
                           :location (routes/absolute-path :email-confirmation))))

(lc/defresource account-acivated
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (-> ctx
                   (aa/build)
                   fv/render-page)))

(defn preserve-session [response request]
  (assoc response :session (:session request)))

(lc/defresource sign-out
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :handle-ok (fn [ctx]
               (-> (routes/absolute-path :index)
                   r/redirect
                   (preserve-session (:request ctx))
                   (update-in [:session] dissoc :signed-in-email)
                   lr/ring-response)))

(lc/defresource forget-secret
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok
  (fn [ctx]
    (-> (routes/absolute-path :account :email (:email ctx))
        r/redirect
        (preserve-session (:request ctx))
        (update-in [:session] dissoc :cookie-data)
        lr/ring-response)))
