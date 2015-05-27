;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

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


(ns freecoin.blockchain
  (:import [freecoin.wallet.wallet])
  (:require
   [freecoin.fxc :as fxc]
   [freecoin.random :as rand]
   [freecoin.params :as param]
   [freecoin.storage :as storage])
  )

(def (storage/connect

(defprotocol Blockchain
  ;; account
  (import-account [bck wallet secret])
  (create-account [bck wallet secret])

  (get-account [bck wallet])
  (get-address [bck wallet])
  (get-balance [bck wallet])

  ;; transactions
  (list-transactions [bck wallet])
  (get-transaction   [bck wallet txid])
  (make-transaction  [bck wallet amount recipient secret])

  ;; vouchers
  (create-voucher [bck wallet amount expiration secret])
  (redeem-voucher [bck wallet voucher])
  )

(defrecord voucher [_id expiration sender
                    amount blockchain currency])

(defrecord transaction
    [_id emission broadcast signed sender
    amount resipient blockchain currency])

(defrecord nxt  [server port])


(defrecord stub [server port]
  Blockchain
  (import-account [bck wallet secrets] nil)

  ;; return an updated wallet map
  (create-account [bck wallet secrets]
    {:pre [(coll? secrets)
           (= 2 (count secrets))
           (contains? wallet :name)]}

    (let [passphrase (fxc/new-passphrase param/encryption (:type bck))                   ;; fake address
          new-bck-pub (assoc-in wallet [:blockchains (keyword (:type bck))] (:string (rand/create 20)))]
      (assoc-in new-bck-pub [:blockchain-secrets (keyword (:type bck))] passphrase)
      ;; TODO: wrap all this with symmetric encryption using secrets
      )
    )

  (get-account [bck wallet]
    (get-in wallet [:blockchains (keyword (:type bck))]))

  (get-address [bck wallet] nil)
  (get-balance [bck wallet] nil)

  (list-transactions [bck wallet] nil)
  (get-transaction   [bck wallet txid] nil)
  (make-transaction  [bck wallet amount recipient secret] nil)

  (create-voucher [bck wallet amount expiration secret] nil)
  (redeem-voucher [bck wallet voucher] nil)
  )


(defn create [type server port]
  (case type
    "NXT"  (conj (nxt.  server port) {:type type})
    "STUB" (conj (stub. server port) {:type type})
    nil)
  )


;; example
;;  (b/create-account (b/_create "STUB" "sadsd" 444)
;;                    (w/new "csdaz" "ca@sdasd") {})


;; (defrecord account
;;     [_id public-key private-key
;;      blockchains blockchain-secrets])
