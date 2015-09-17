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
            [freecoin.db.wallet :as wallet]
            [freecoin.db.confirmation :as confirmation]
            [freecoin.db.uuid :as uuid]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.views.transaction-form :as transaction-form]
            [freecoin.views.confirm-transaction-form :as confirm-transaction-form]))

(lc/defresource get-transaction-form [wallet-store]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (and (wallet/fetch wallet-store uid)
                              (ch/context->cookie-data ctx)) true)))
  :handle-ok (fn [ctx]
               (-> {}
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
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (and (wallet/fetch wallet-store uid)
                              (ch/context->cookie-data ctx)) true)))
  :allowed? (fn [ctx]
              (let [{:keys [status data problems]}
                    (validate-form transaction-form/transaction-form-spec
                                   (ch/context->params ctx))]
                (when (= :ok status)
                  (when-let [recipient-wallet (wallet/fetch wallet-store (:recipient data))]
                    true))))
  :post! (fn [ctx]
           (let [amount (get-in ctx [::form-data :amount])
                 recipient-uid (get-in ctx [::form-data :recipient])
                 sender-uid (ch/context->signed-in-uid ctx)]
             (when-let [confirmation (confirmation/new-transaction-confirmation!
                                      confirmation-store uuid/uuid
                                      sender-uid recipient-uid amount)]
               {::confirmation confirmation})))
  :handle-forbidden (lr/ring-response (r/redirect "/get-transaction-form")))

(lc/defresource get-confirm-transaction-form [confirmation-store]
  :allowed-methods [:get]
  :exists? (fn [ctx]
             (let [confirmation-uid (:confirmation-uid (ch/context->params ctx))
                   signed-in-uid (ch/context->signed-in-uid ctx)]
               (when-let [confirmation (confirmation/fetch confirmation-store confirmation-uid)]
                 (when (= signed-in-uid (get-in confirmation [:data :sender-uid]))
                   true))))
  :handle-ok (fn [ctx]
               (-> {}
                   confirm-transaction-form/build
                   fv/render-page)))
