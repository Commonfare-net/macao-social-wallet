;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Arjan Scherpenisse <arjan@scherpenisse.net>

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

(ns freecoin.views.transaction-list
  (:require [freecoin.routes :as routes]
            [freecoin.db.wallet :as wallet]
            [clojure.tools.logging :as log]
            [freecoin.config :as config]))

(defn build [list wallet-store & [owner-wallet]]
  (let [title (str "Transaction list" (when (not (nil? owner-wallet)) (str " for " (:name owner-wallet))))]
    (log/info "owner-wallet" owner-wallet)

    {:title title
     :heading title
     :body-class "func--transactions-page--body"
     :body
     [:table.func--transactions-page--table.table.table-striped
      [:thead
       [:tr
        [:th "From"]
        [:th "To"]
        [:th "Amount"]
        [:th "Time"]]]
      [:tbody
       (map (fn [t]
              (let [from (wallet/fetch-by-account-id wallet-store (:from-id t))
                    to (wallet/fetch-by-account-id wallet-store (:to-id t))]
                [:tr
                 [:td [:a {:href (routes/path :account :uid (:uid from))} (:name from)]]
                 [:td [:a {:href (routes/path :account :uid (:uid to))} (:name to)]]
                 [:td (:amount t)]
                 [:td (:timestamp t)]]))
            list)
       ]
      ]
     }))
