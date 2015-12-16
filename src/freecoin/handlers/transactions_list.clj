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

(ns freecoin.handlers.transactions-list
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


(lc/defresource list-user-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists?
  (fn [ctx]
    (when-let [uid (:uid (ch/context->params ctx))]
      (when-let [wallet (wallet/fetch wallet-store uid)]
        {::wallet wallet})))

  :handle-ok
  (fn [ctx]
    (-> blockchain
        (blockchain/list-transactions (-> ctx ::wallet :account-id))
        (transaction-list/build-html wallet-store (::wallet ctx))
        fv/render-page)))

(lc/defresource list-all-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok
  (fn [ctx]
    (-> blockchain
        (blockchain/list-transactions)
        (transaction-list/build-html wallet-store)
        fv/render-page)))

(lc/defresource list-all-activity-streams [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["application/json"]
  ;; TODO: register the mooncake authorised to pull
  :handle-ok
  (fn [ctx]
      (-> blockchain
          (blockchain/list-transactions)
          (transaction-list/build-activity-stream wallet-store)
          )
      ))
