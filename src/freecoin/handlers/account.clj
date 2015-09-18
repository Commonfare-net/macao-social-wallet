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

(ns freecoin.handlers.account
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [freecoin.db.wallet :as wallet]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.views.balance-page :as balance-page]))

(lc/defresource balance-page [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? (fn [ctx]
             (if-let [uid (ch/context->signed-in-uid ctx)]
               (let [wallet (wallet/fetch wallet-store uid)]
                 {::wallet wallet})))
  :handle-ok (fn [ctx]
               (if-let [wallet (::wallet ctx)]
                 (-> {:wallet wallet :balance (blockchain/get-balance blockchain (:account-id wallet))}
                     balance-page/balance-page
                     fv/render-page)
                 (lr/ring-response (r/redirect "/landing-page")))))
