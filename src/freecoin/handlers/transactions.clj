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
            [freecoin.db.wallet :as wallet]
            [freecoin.context-helpers :as ch]
            [freecoin.views :as fv]
            [freecoin.views.transaction-form :as transaction-form]))

(lc/defresource get-transaction-form [wallet-store]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (and (wallet/fetch wallet-store uid)
                              (ch/context->cookie-data ctx)) true)))
  :handle-ok (fn [ctx]
               (-> {}
                   transaction-form/build
                   fv/render-page)))

(lc/defresource post-transaction-form [wallet-store]
  :allowed-methods [:post]
  :authorized? (fn [ctx]
                 (when-let [uid (ch/context->signed-in-uid ctx)]
                   (when (and (wallet/fetch wallet-store uid)
                              (ch/context->cookie-data ctx)) true))))
