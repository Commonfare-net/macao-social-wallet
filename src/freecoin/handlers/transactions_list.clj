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
  (:require [taoensso.timbre :as log]
            [freecoin.auth :as auth]
            [freecoin.blockchain :as blockchain]
            [freecoin.context-helpers :as ch]
            [freecoin.db.wallet :as wallet]
            [freecoin.views :as fv]
            [freecoin.views.transaction-list :as transaction-list]
            [liberator.core :as lc]))

(defn parse-tags
  "Transform tags coming as input parameters to a REST endpoint into a set of strings"
  [tags]
  (cond
    (string? tags) (if (seq tags) #{tags} #{})
    (set? tags)   tags
    (coll? tags)  (into #{} tags)
    :else         #{}))

(defn maybe-merge
  "Merges a key val pair into map if val is truthy or satisfies pred."
  ([m key val]
   (maybe-merge m key val identity))
  ([m key val pred]
   (if (pred val)
     (assoc m key val)
     m)))

(lc/defresource list-user-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :exists? #(auth/has-wallet % wallet-store)

  :handle-ok
  (fn [ctx]
    (let [tags (-> ctx :request :params :tags parse-tags)]
      (-> blockchain
          (blockchain/list-transactions (-> {:account-id (-> ctx :wallet :account-id)}
                                            (maybe-merge :tags tags seq)))
          (transaction-list/build-html tags wallet-store (:wallet ctx))
          fv/render-page))))

(lc/defresource list-all-transactions [wallet-store blockchain]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? #(auth/is-signed-in %)

  :handle-ok
  (fn [ctx]
    (let [tags (-> ctx :request :params :tags parse-tags)]
      (-> blockchain
          (blockchain/list-transactions (-> ctx :request :params
                                            (maybe-merge :tags tags seq)))
          (transaction-list/build-html tags wallet-store)
          fv/render-page))))

(lc/defresource list-all-activity-streams [wallet-store blockchain]
  :allowed-methods [:get]
  ;; Activity Streams 2.0 specification says media type should be application/activity+json
  :available-media-types ["application/activity+json" "application/json"]
  ;; TODO: register the mooncake authorised to pull
  :handle-ok
  (fn [ctx]
    (let [transactions
          (blockchain/list-transactions blockchain (-> ctx :request :params))]
        (transaction-list/build-activity-stream transactions wallet-store))))
