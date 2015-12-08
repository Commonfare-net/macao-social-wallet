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

(ns freecoin.auth
  (:require [clojure.string :as str]
            [freecoin.storage :as storage]
            [freecoin.db.wallet :as wallet]))

;; TODO:  use throw/catch exceptions
;; which will then go to :handle-exception

(defn get-apikey [request]
  {:pre [(contains? request :session)]}

  (let [cookie (get-in request [:session :cookie-data])]
    (if (empty? cookie) {}
        (let [toks (str/split cookie #"::")]
          (if (< (count toks) 2) {}
              ;; else
              {:_id (second toks)
               :slice (first toks)}

              )))))


(defn get-wallet
  ([request]
   (get-wallet request (get-apikey request)))

  ([request apikey]
   {:pre [(contains? request :config)
          (contains? (:config request) :db-connection)
          (contains? apikey :_id)]}


   (let [id (:_id apikey)
         db (get-in request [:config :db-connection])
         wallet (storage/find-by-id (:db db) "wallets" id)]

     (if (empty? wallet) false
         ;; else
         wallet
         )
     )
   ))


(defn get-secret
  ([request]
   (get-secret request (get-apikey request)))

  ([request apikey]
   {:pre [(contains? request :config)
          (contains? apikey :_id)]}

   (let [id (:_id apikey)
         db (get-in request [:config :db-connection])
         secret (storage/find-by-id (:db db) "secrets" id)]
     ;; safeguard
     (if (empty? secret) false
         ;; else return the
         secret))
   ))

(defn check
  ([wallet-store uid]
   (if-let [wallet (wallet/fetch wallet-store uid)]
     {:result true
      :wallet wallet}
     {:result false
      :problem "Authorization failed: wallet not found in database."}))
  
  ([request]
   (let [apikey (get-apikey request)]
     (if (empty? apikey)
       {:result false
        :problem "Authorization failed: no cookie found in session."}
       (let [wallet (get-wallet request apikey)]
         (if (empty? wallet)
           {:result false
            :problem "Authorization failed: wallet not found in database."}
           {:result true
            :wallet wallet
            :apikey apikey}))))))
