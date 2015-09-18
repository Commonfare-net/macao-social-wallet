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
            [freecoin.db.uuid :as uuid]
            [freecoin.db.wallet :as wallet]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]
            [freecoin.views.balance-page :as balance-page]))

(lc/defresource landing-page [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? (fn [ctx]
             (if-let [uid (ch/context->signed-in-uid ctx)]
               (let [wallet (wallet/fetch wallet-store uid)]
                 {::wallet wallet
                  ::balance (blockchain/get-balance blockchain (:account-id wallet))})
               {}))
  :handle-ok (fn [ctx]
               (if-let [wallet (::wallet ctx)]
                 (-> {:wallet wallet :balance (::balance ctx)}
                     balance-page/balance-page
                     fv/render-page)
                 (-> {:sign-in-url "/sign-in-with-sso"}
                     landing-page/landing-page
                     fv/render-page))))

(lc/defresource sign-in [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (lr/ring-response (soc/authorisation-redirect-response sso-config))))

(defn wallet->access-key [blockchain wallet]
  (let [secret (get-in wallet [:blockchain-secrets (blockchain/label blockchain)])]
    (s/join "::" [(:cookie secret) (:_id secret)])))

(lc/defresource sso-callback [wallet-store blockchain sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :allowed? (fn [ctx]
              (when-let [code (get-in ctx [:request :params :code])]
                (try
                  (when-let [token-response (soc/request-access-token! sso-config code)]
                    {::token-response token-response})
                  (catch Exception e nil))))
  :handle-forbidden (lr/ring-response (r/redirect "/landing-page"))
  :exists? (fn [ctx]
             (let [token-response (::token-response ctx)
                   sso-id (get-in token-response [:user-info :sub])
                   email (get-in token-response [:user-info :email])
                   name (first (s/split email #"@"))]
               (if-let [wallet (wallet/fetch-by-sso-id wallet-store sso-id)]
                 {::uid (:uid wallet)}
                 (when-let [{:keys [wallet apikey]} (wallet/new-empty-wallet! wallet-store blockchain uuid/uuid
                                                                              sso-id name email)]
                   {::uid (:uid wallet)
                    ::cookie-data apikey}))))
  :handle-ok (fn [ctx]
               (lr/ring-response
                (cond-> (r/redirect "/")
                  (::cookie-data ctx) (assoc-in [:session :cookie-data] (::cookie-data ctx))
                  true (assoc-in [:session :signed-in-uid] (::uid ctx)))))
  :handle-not-found (lr/ring-response (r/redirect "/landing-page")))
