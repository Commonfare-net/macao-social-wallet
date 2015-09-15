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

(ns freecoin.handlers.participant-query
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [clojure.string :as s]
            [freecoin.utils :as utils]
            [freecoin.db.wallet :as wallet]
            [freecoin.storage :as storage]
            [freecoin.views :as fv]
            [freecoin.views.participants-list :as participants-list]))

(defn context->wallet-query [context]
  (if-let [{:keys [field value]} (-> context
                                     (get-in [:request :params])
                                     (utils/select-all-or-nothing [:field :value]))]
    {(keyword field) value}
    {}))

(lc/defresource participants [wallet-store]
  :exists? (fn [ctx]
             {::wallets (->> ctx
                             context->wallet-query
                             (wallet/query wallet-store))})
  :handle-ok (fn [ctx]
               (-> {:wallets (::wallets ctx)}
                   participants-list/participants-list
                   fv/render-page)))

