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
            [freecoin.db.wallet :as wallet]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.auth :as auth]
            [freecoin.views.transaction-list :as transaction-list]))


(lc/defresource list-user-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :exists? #(auth/has-wallet % wallet-store)

  :handle-ok
  (fn [ctx]
    (-> blockchain
        (blockchain/list-transactions (-> ctx :wallet :account-id))
        (transaction-list/build-html wallet-store (:wallet ctx))
        fv/render-page)))

(lc/defresource list-all-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok
  (fn [ctx]
    (-> blockchain
        (blockchain/list-transactions)
        (transaction-list/build-html wallet-store)
        fv/render-page)))

(lc/defresource list-all-activity-streams [wallet-store blockchain]
  :allowed-methods [:get]
  ;; Activity Streams 2.0 specification says media type should be application/activity+json
  :available-media-types ["application/json"]
  ;; TODO: register the mooncake authorised to pull
  :handle-ok
  (fn [ctx]
      (-> blockchain
          (blockchain/list-transactions)
          (transaction-list/build-activity-stream wallet-store)
          )
      ))
