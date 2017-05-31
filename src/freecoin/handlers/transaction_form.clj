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

(ns freecoin.handlers.transaction-form
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [freecoin.db.wallet :as wallet]
            [freecoin.db.confirmation :as confirmation]
            [freecoin.db.uuid :as uuid]
            [freecoin.context-helpers :as ch]
            [freecoin.routes :as routes]
            [freecoin.blockchain :as blockchain]
            [freecoin.auth :as auth]
            [freecoin.config :as config]
            [freecoin.views :as fv]
            [freecoin.form_helpers :as fh]
            [freecoin.views.transaction-form :as transaction-form]
            [taoensso.timbre :as log]))

(lc/defresource get-transaction-form [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok (fn [ctx]
               (-> (:request ctx)
                   transaction-form/build
                   fv/render-page)))

(lc/defresource post-transaction-form [blockchain wallet-store confirmation-store]
  :allowed-methods [:post]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :allowed?
  (fn [ctx]
    (let [{:keys [status data problems]}
          (fh/validate-form transaction-form/transaction-form-spec
                            (ch/context->params ctx))
          amount (:amount data)
          sender-email (ch/context->signed-in-email ctx)
          sender-balance (blockchain/get-balance blockchain (:account-id (wallet/fetch wallet-store sender-email)))
          admin-email (or (-> (config/create-config) config/admin-email) "")]
      (if (= :ok status)
        (if-let [recipient-wallet
                 (wallet/fetch wallet-store (:recipient data))]
          ;; Check that the balance aftter the transaction would be above 0 unless it is made by the admin
          (if (or (=  admin-email sender-email)
                  (>= (- sender-balance amount) 0)) 
            {::form-data data
             ::recipient-wallet recipient-wallet}
            [false (fh/form-problem :amount "Balance is not sufficient to perform this transaction")])
          [false (fh/form-problem :recipient "Not found")])
        [false (fh/form-problem problems)])))

  :post!
  (fn [ctx]
    (let [amount (get-in ctx [::form-data :amount])
          tags (get-in ctx [::form-data :tags] #{})
          sender-email (ch/context->signed-in-email ctx)
          recipient (::recipient-wallet ctx)]
      (when-let [confirmation (confirmation/new-transaction-confirmation!
                               confirmation-store uuid/uuid
                               sender-email (:email recipient) amount tags)]
        {::confirmation confirmation})))

  :post-redirect?
  (fn [ctx]
    {:location (routes/absolute-path
                :get-confirm-transaction-form
                :confirmation-uid (:uid (::confirmation ctx)))})

  :handle-forbidden
  (fn [ctx]
    (-> (routes/absolute-path :get-transaction-form)
        r/redirect
        (fh/flash-form-problem ctx)
        lr/ring-response)))
