;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>
;; Andrei Biasprozvanny <abiaspro@thoughtworks.com>

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

(ns freecoin.handlers.confirm-transaction-form
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [freecoin.db.wallet :as wallet]
            [freecoin.db.confirmation :as confirmation]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.views :as fv]
            [freecoin.auth :as auth]
            [freecoin.form_helpers :as fh]
            [freecoin.views.confirm-transaction-form
             :as confirm-transaction-form]))

(lc/defresource get-confirm-transaction-form [wallet-store confirmation-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :authorized? #(auth/is-signed-in %)

  :exists?
  (fn [ctx]
    (let [confirmation-uid (:confirmation-uid (ch/context->params ctx))
          signed-in-email (:email ctx)]
      (when-let [confirmation
                 (confirmation/fetch confirmation-store confirmation-uid)]
        (when (= signed-in-email (get-in confirmation [:data :sender-email]))
          {::confirmation confirmation}))))

  :handle-ok
  (fn [ctx]
    (let [confirmation (::confirmation ctx)
          recipient (wallet/fetch wallet-store
                      (get-in ctx [::confirmation :data :recipient-email]))
          show-pin-entry (= nil (ch/context->cookie-data ctx))]
      (-> {:confirmation confirmation
           :recipient recipient
           :request (:request ctx)}
          (confirm-transaction-form/build show-pin-entry)
          fv/render-page))))

(defn preserve-session [response {:keys [session]}]
  (assoc response :session session))

(lc/defresource post-confirm-transaction-form
  [wallet-store confirmation-store blockchain]
  :allowed-methods [:post]
  :available-media-types ["text/html"]

  ;; TODO: fix use  of authorized and exists here conforming to auth functions
  ;; when we tried last moving below to exists? (and lower handlers to not-found)
  ;; did trigger several errors in tests and even some compiling errors.

  :authorized?
  (fn [ctx]
    (let [signed-in-email (ch/context->signed-in-email ctx)
          sender-wallet (wallet/fetch wallet-store signed-in-email)
          confirmation-uid (:confirmation-uid (ch/context->params ctx))
          confirmation (confirmation/fetch confirmation-store confirmation-uid)]

      (when (and sender-wallet
                 confirmation
                 (= signed-in-email (-> confirmation :data :sender-email)))
        {::confirmation confirmation
         ::sender-wallet sender-wallet})))

  :allowed?
  (fn [ctx]
    (if-let [secret (ch/context->cookie-data ctx)]
      ;; pre-authorized
      {::secret secret}
      ;; PIN entered in form
      (let [confirmation (::confirmation ctx)
            {:keys [status data problems]}
            (fh/validate-form (confirm-transaction-form/confirm-transaction-form-spec (:uid confirmation) true)
                              (ch/context->params ctx))]
        (if (= :ok status)
          {::secret (:secret data)}
          [false (fh/form-problem problems)]))))

  :post!
  (fn [ctx]
    (let [{:keys [sender-email recipient-email amount tags]}
          (-> ctx ::confirmation :data)
          sender-wallet (wallet/fetch wallet-store sender-email)
          recipient-wallet (wallet/fetch wallet-store recipient-email)
          secret (::secret ctx)]
      (blockchain/make-transaction blockchain
                                   (:account-id sender-wallet) amount
                                   (:account-id recipient-wallet) {:secret secret
                                                                   :tags tags})
      (confirmation/delete!
       confirmation-store
       (-> ctx ::confirmation :uid))

      {::email (:email sender-wallet) ::secret secret}))

  :post-redirect?
  (fn [ctx]
    {:location (routes/absolute-path :account :email (::email ctx))})

  :handle-see-other
  (fn [ctx]
    (->
     (:location ctx)
     r/redirect
     (preserve-session (:request ctx))
     (update-in [:session] assoc :cookie-data (::secret ctx))
     lr/ring-response))

  :handle-forbidden
  (fn [ctx]
    (-> (routes/absolute-path :get-confirm-transaction-form :confirmation-uid (-> ctx ::confirmation :uid))
        r/redirect
        (fh/flash-form-problem ctx)
        lr/ring-response)))
