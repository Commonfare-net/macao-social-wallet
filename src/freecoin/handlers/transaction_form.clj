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
            [freecoin.auth :as auth]
            [freecoin.config :as config]
            [freecoin.views :as fv]
            [freecoin.form_helpers :as fh]
            [freecoin.views.transaction-form :as transaction-form]))

(lc/defresource get-transaction-form [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok (fn [ctx]
               (-> (:request ctx)
                   transaction-form/build
                   fv/render-page)))

(lc/defresource post-transaction-form [wallet-store confirmation-store]
  :allowed-methods [:post]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :allowed?
  (fn [ctx]
    (let [{:keys [status data problems]}
          (fh/validate-form transaction-form/transaction-form-spec
                            (ch/context->params ctx))]
      (if (= :ok status)
        (if-let [recipient-wallet
                 (wallet/fetch-by-name wallet-store (:recipient data))]
          {::form-data data
           ::recipient-wallet recipient-wallet}
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
