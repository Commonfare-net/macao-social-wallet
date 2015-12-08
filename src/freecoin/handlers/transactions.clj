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

(ns freecoin.handlers.transactions
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [formidable.parse :as fp]
            [clojure.tools.logging :as log]
            [freecoin.db.wallet :as wallet]
            [freecoin.db.confirmation :as confirmation]
            [freecoin.blockchain :as blockchain]
            [freecoin.db.uuid :as uuid]
            [freecoin.context-helpers :as ch]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.views :as fv]
            [freecoin.form_helpers :as fh]
            [freecoin.views.transaction-form :as transaction-form]
            [freecoin.views.transaction-list :as transaction-list]
            [freecoin.views.confirm-transaction-form
             :as confirm-transaction-form]))

(lc/defresource get-transaction-form [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (wallet/fetch wallet-store uid) true)))

  :handle-ok (fn [ctx]
               (-> (:request ctx)
                   transaction-form/build
                   fv/render-page)))

(defn validate-form [form-spec data]
  (fp/with-fallback
    (fn [problems] {:status :error
                    :problems problems})
    {:status :ok
     :data (fp/parse-params form-spec data)}))

(lc/defresource post-transaction-form [wallet-store confirmation-store]
  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (wallet/fetch wallet-store uid) true)))

  :allowed?
  (fn [ctx]
    (let [{:keys [status data problems]}
          (validate-form transaction-form/transaction-form-spec
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
          sender-uid (ch/context->signed-in-uid ctx)
          recipient (::recipient-wallet ctx)
          ]
      (when-let [confirmation (confirmation/new-transaction-confirmation!
                               confirmation-store uuid/uuid
                               sender-uid (:uid recipient) amount)]
        {::confirmation confirmation})))

  :post-redirect?
  (fn [ctx]
    {:location (routes/absolute-path
                (config/create-config)
                :get-confirm-transaction-form
                :confirmation-uid (:uid (::confirmation ctx)))})

  :handle-forbidden
  (fn [ctx]
    (-> (routes/absolute-path (config/create-config) :get-transaction-form)
        r/redirect
        (fh/flash-form-problem ctx)
        lr/ring-response
        )
    ))

(lc/defresource get-confirm-transaction-form [wallet-store confirmation-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists?
  (fn [ctx]
    (let [confirmation-uid (:confirmation-uid (ch/context->params ctx))
          signed-in-uid (ch/context->signed-in-uid ctx)]
      (when-let [confirmation
                 (confirmation/fetch confirmation-store confirmation-uid)]
        (when (= signed-in-uid (get-in confirmation [:data :sender-uid]))
          {::confirmation confirmation}))))
  :handle-ok
  (fn [ctx]
    (let [confirmation (::confirmation ctx)
          recipient (wallet/fetch wallet-store
                      (get-in ctx [::confirmation :data :recipient-uid]))]

      (-> {:confirmation confirmation
           :recipient recipient}
          confirm-transaction-form/build
          fv/render-page)))
  )

(lc/defresource post-confirm-transaction-form
  [wallet-store confirmation-store blockchain]
  :allowed-methods [:post]
  :authorized?
  (fn [ctx]
    (let [signed-in-uid (ch/context->signed-in-uid ctx)
          sender-wallet (wallet/fetch wallet-store signed-in-uid)
          confirmation-uid (:confirmation-uid (ch/context->params ctx))
          confirmation (confirmation/fetch confirmation-store confirmation-uid)]

      (when (and sender-wallet
                 confirmation
                 (= signed-in-uid (-> confirmation :data :sender-uid)))
        {::confirmation confirmation
         ::sender-wallet sender-wallet})))

  :allowed? (fn [ctx]
              (when-let [secret (ch/context->cookie-data ctx)]
                {::secret secret}))

  :post! (fn [ctx]
           (let [{:keys [sender-uid recipient-uid amount]}
                 (-> ctx ::confirmation :data)
                 sender-wallet (wallet/fetch wallet-store sender-uid)
                 recipient-wallet (wallet/fetch wallet-store recipient-uid)
                 secret (ch/context->cookie-data ctx)]
             (blockchain/make-transaction blockchain
                                          (:account-id sender-wallet) amount
                                          (:account-id recipient-wallet) secret)
             (confirmation/delete!
              confirmation-store
              (-> ctx ::confirmation :uid))

             {::sender-uid (:uid sender-wallet)}))
  :post-redirect? (fn [ctx] {:location
                             (routes/absolute-path
                              (config/create-config) :account
                              :uid (::sender-uid ctx))}))

(lc/defresource list-user-transactions [wallet-store confirmation-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when-let [wallet (wallet/fetch wallet-store uid)]
                     {::wallet wallet})))

  :handle-ok (fn [ctx]
               (-> confirmation-store
                   (confirmation/get-confirmations-by-uid (-> ctx ::wallet :uid))
                   transaction-list/build
                   fv/render-page)))
