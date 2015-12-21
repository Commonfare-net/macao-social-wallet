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

(ns freecoin.handlers.participants
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [clojure.tools.logging :as log]
            [freecoin.utils :as utils]
            [freecoin.auth :as auth]
            [freecoin.db.wallet :as wallet]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.views.participants-query-form :as participants-query-form]
            [freecoin.views.participants-list :as participants-list]
            [freecoin.views.account-page :as account-page]))

(lc/defresource account [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :exists? #(auth/has-wallet % wallet-store)

  :handle-ok (fn [ctx]
    (if-let [wallet (:wallet ctx)]
      (-> {:wallet wallet
           :balance (blockchain/get-balance blockchain (:account-id wallet))}
          account-page/build
          fv/render-page)
      (lr/ring-response (r/redirect "/landing-page")))))

(lc/defresource query-form [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok (fn [ctx]
               (-> {}
                   participants-query-form/build
                   fv/render-page)))

(defn request->wallet-query [request]
  (if-let [{:keys [field value]}
           (-> request :params
               (utils/select-all-or-nothing [:field :value]))]
    {(keyword field) value}
    {}))

(lc/defresource participants [wallet-store]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok (fn [ctx]
               (-> {:wallets (wallet/query wallet-store (request->wallet-query (:request ctx)))}
                   participants-list/participants-list
                   fv/render-page)))
