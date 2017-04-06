;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

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
            [stonecutter-oauth.client :as soc]
            [freecoin.routes :as routes]
            [freecoin.db.wallet :as wallet]
            [freecoin.db.mongo :as mongo]
            [freecoin.auth :as auth]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]
            [freecoin.views.index-page :as index-page]
            [freecoin.views.sign-in :as sign-in-page]
            [freecoin.form_helpers :as fh]
            [freecoin.context-helpers :as ch]
            [taoensso.timbre :as log]))

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
                 (log/info "##############")
                 (let [{:keys [status data problems]}
                       (fh/validate-form sign-in-page/sign-in-form
                                         (ch/context->params ctx))]
                   (if (= :ok (log/spy status))
                     (let [email (-> ctx :request :params :user-name)]
                       (when-let [account (mongo/fetch account-store email)]
                         (when (= (-> ctx :request :params :user-name) (:password account))
                           {:email (:email account)})))
                     ;; It never gets here cause it rdirects to handle-unauthorised or its default
                     (do
                       (log/info (str "Problems: " (clojure.pprint/pprint problems)))
                       [false (fh/form-problem (log/spy problems))]))))

  :handle-unauthorized (fn [ctx]
                         (lr/ring-response (fh/flash-form-problem
                                            (r/redirect (routes/absolute-path :sign-in))
                                            ctx)))

  :exists? (fn [ctx]
             ;; the wallet exists already
             (let [email (:email ctx)]
               (if-let [wallet (wallet/fetch wallet-store "account")]
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
                   {::email (:email wallet)}))))

  :handle-ok (fn [ctx]
               (lr/ring-response
                (cond-> (r/redirect (routes/absolute-path :account :email (::email ctx)))
                  (::cookie-data ctx) (assoc-in [:session :cookie-data] (::cookie-data ctx))
                  true (assoc-in [:session :signed-in-email] (::email ctx)))))

  ;; TODO: Maybe add not found text to landing page?
  :handle-not-found (-> (routes/absolute-path :landing-page)
                        r/redirect
                        lr/ring-response))

(lc/defresource create-account [account-store]
  :allowed-methods [:post]
  :available-media-types ["text/html"]

  :post! (fn [ctx]
           ;; TODO
           )

  :post-redirect?
  ;; TODO: this should be replaced with a confirmation page, while waiting for the email confirmation
  (fn [ctx]
    {:location (routes/absolute-path :account :email (::email ctx))}))

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
