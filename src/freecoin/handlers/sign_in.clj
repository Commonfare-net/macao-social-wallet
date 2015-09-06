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
            [stonecutter-oauth.jwt :as sjwt]
            [freecoin.db.participant :as participant]
            [freecoin.db.wallet :as wallet]
            [freecoin.storage :as storage]
            [freecoin.blockchain :as blockchain]
            [freecoin.utils :as utils]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]))

(lc/defresource landing-page
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> {:sign-in-url "/sign-in-with-sso"}
                 landing-page/landing-page
                 fv/render-page))

(lc/defresource sign-in [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (lr/ring-response (soc/authorisation-redirect-response sso-config))))

(defn empty-wallet [name email]
  {:_id ""            ;; unique id
   :name  name        ;; identifier, case insensitive, space counts
   :email email       ;; verified email account
   :info nil          ;; misc information text on the account
   :creation-date nil ;; date on which the wallet was created
   :last-login nil    ;; last time this participant logged in succesfully
   :last-login-ip nil ;; connection ip address of the last succesful login
   :failed-logins nil ;; how many consecutive failed logins were attempted
   :public-key nil    ;; public asymmetric key for off-the-blockchain encryption
   :private-key nil   ;; private asymmetric key for off-the-blockchain encryption
   :blockchains {}       ;; list of blockchains and public account ids
   :blockchain-keys {}}) ;; list of keys for private blockchain operations

(defn create-wallet [db-connection name email]
  (let [new-account (blockchain/create-account
                     (blockchain/new-stub db-connection)
                     wallet/empty-wallet)
        secret (get-in new-account [:blockchain-secrets :STUB])
        secret-without-cookie (dissoc secret :cookie)
        wallet-access-key (s/join "::" [(:cookie secret) (:_id secret)])]
    (if (contains? new-account :problem)
      (do
        (utils/log! (::error new-account)))
      (do
        (storage/insert db-connection "wallets"
                        (assoc new-account :_id (:_id secret)))
        {:wallet new-account
         :wallet-access-key wallet-access-key}))))

(lc/defresource sso-callback [db-connection participant-store wallet-store blockchain sso-config]
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
                   sso-id (get-in token-response [:user-info :user-id])
                   email (get-in token-response [:user-info :email])
                   email-verified (get-in token-response [:user-info :email_verified])
                   name (first (s/split email #"@"))]
               (if-let [participant (participant/fetch-by-sso-id participant-store sso-id)]
                 {::uid (:uid participant)
                  ::wallet-id (:wallet-id participant)}
                 (when-let [{:keys [wallet wallet-access-key]} (create-wallet db-connection name email)]
                   (let [participant (participant/store! participant-store sso-id name email wallet)]
                     {::uid (:uid participant)
                      ::cookie-data wallet-access-key})))))
  :handle-ok (fn [ctx]
               (lr/ring-response
                (-> (r/redirect "/landing-page")
                    (assoc-in [:session :cookie-data] (::cookie-data ctx))
                    (assoc-in [:session :signed-in-uid] (::uid ctx)))))
  :handle-not-found (lr/ring-response (r/redirect "/landing-page")))
