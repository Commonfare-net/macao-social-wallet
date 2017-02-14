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

(ns freecoin.db.wallet
  (:require [freecoin.blockchain :as blockchain]
            [freecoin.db.mongo :as mongo]))

(defn- empty-wallet [sso-id name email]
  {:sso-id sso-id     ;; id from single sign-on service
   :name  name        ;; identifier, case insensitive, space counts
   :email email       ;; verified email account
;   :info nil          ;; misc information text on the account
;   :creation-date nil ;; date on which the wallet was created
;   :last-login nil    ;; last time this participant logged in succesfully
;   :last-login-ip nil ;; connection ip address of the last succesful login
;   :failed-logins nil ;; how many consecutive failed logins were attempted
   :public-key nil    ;; public asymmetric key for off-the-blockchain encryption
   :private-key nil   ;; private asymmetric key for off-the-blockchain encryption
   :account-id nil    ;; blockchain account id
   })

(defn secret->apikey [secret]
  (str (:cookie secret) "::" (:_id secret)))

(defn secret->participant-shares [secret]
  (take 4 (:slices secret)))

(defn secret->organization-shares [secret]
  (->> (:slices secret)
       (drop 3) (take 4)))

(defn secret->auditor-shares [secret]
  (take-last 3 (:slices secret)))

(defn new-empty-wallet! [wallet-store blockchain sso-id name email]
  (let [{:keys [account-id account-secret]} (blockchain/create-account blockchain)
        wallet (-> (empty-wallet sso-id name email)
                   (assoc :account-id account-id))]
    {:wallet       (mongo/store! wallet-store :email wallet)
     :apikey       (secret->apikey              account-secret)
     :participant  (secret->participant-shares  account-secret)
     :organization (secret->organization-shares account-secret)
     :auditor      (secret->auditor-shares      account-secret)}))

(defn fetch [wallet-store email]
  (mongo/fetch wallet-store email))

(defn fetch-by-sso-id [wallet-store sso-id]
  (first (mongo/query wallet-store {:sso-id sso-id})))

(defn fetch-by-email [wallet-store email]
  (first (mongo/query wallet-store {:email email})))

(defn fetch-by-name [wallet-store name]
  (first (mongo/query wallet-store {:name name})))

(defn fetch-by-account-id [wallet-store name]
  (first (mongo/query wallet-store {:account-id name})))

(defn query
  ([wallet-store] (query wallet-store {}))
  ([wallet-store query-m] (mongo/query wallet-store query-m)))
