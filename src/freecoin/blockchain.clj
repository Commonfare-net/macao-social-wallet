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
  (:require [clojure.string :as str]
            [freecoin.fxc :as fxc]
            [freecoin.params :as param]
            [freecoin.storage :as storage]
            [simple-time.core :as time]))

(defprotocol Blockchain
  ;; blockchain identifier
  (label [bk])
  
  ;; account
  (import-account [bk account-id secret])
  (create-account [bk])

  (get-address [bk account-id])
  (get-balance [bk account-id])

  ;; transactions
  (list-transactions [bk account-id])
  (get-transaction   [bk account-id txid])
  (make-transaction  [bk from-account-id amount to-account-id secret])

  ;; vouchers
  (create-voucher [bk account-id amount expiration secret])
  (redeem-voucher [bk account-id voucher]))

(defrecord voucher
    [_id
     expiration
     sender
     amount
     blockchain
     currency])

(defrecord transaction
    [_id
     emission
     broadcast
     signed
     sender
     amount
     recipient
     blockchain
     currency])

;; this is here jut to explore how introspection works in clojure records
;; basically one could just explicit the string "STUB" where this is used
(defn recname [record]
  "Return a string which is the name of the record class, uppercase. Used to identify the class type."
  (str/upper-case (last (str/split (pr-str (class record)) #"\.")))
  )

;; TODO
(defrecord nxt [server port])

;; inherits from Blockchain and implements its methods
(defrecord Stub [db]
  Blockchain
  (label [bk] (keyword (recname bk)))
  
  (import-account [bk account-id secrets] nil)

  (create-account [bk]
    (let [secret (fxc/create-secret param/encryption (recname bk))]
      {:account-id (:_id secret)
       :account-secret secret}
      ;; TODO: wrap all this with symmetric encryption using secrets
      ))

  (get-address [bk account-id] nil)
  (get-balance [bk account-id]
    ;; we use the aggregate function in mongodb, sort of simplified map/reduce
    (let [received-map (first (storage/aggregate db "transactions"
                                      [{"$group" {:_id "$to"
                                                  :total {"$sum" "$amount"}}}
                                       {"$match" {:_id account-id}}]))
          sent-map  (first (storage/aggregate db "transactions"
                                   [{"$group" {:_id "$from"
                                               :total {"$sum" "$amount"}}}
                                    {"$match" {:_id account-id}}]))
          received (if (nil? received-map) 0 (:total received-map))
          sent      (if (nil? sent-map) 0 (:total sent-map))]
      ;; return the balance
      (- received sent)
      ))
      
  (list-transactions [bk account-id] (storage/find-by-key db "transactions" {:blockchain "STUB"}))

  (get-transaction   [bk account-id txid] nil)

  (make-transaction  [bk from-account-id amount to-account-id secret]
    (let [now (time/format (time/now))]
      ;; TODO: Keep track of accounts to verify validity of from- and
      ;; to- accounts
      (storage/insert db "transactions"
       {:_id (str now "-" from-account-id)
        :blockchain "STUB"
        :timestamp now
        :from-id from-account-id
        :to-id to-account-id
        :amount amount})))

  (create-voucher [bk account-id amount expiration secret] nil)
  
  (redeem-voucher [bk account-id voucher] nil))

(defn new-stub [db]
  "Check that the blockchain is available, then return a record"
  (Stub. db))

;;; in-memory blockchain for testing
(defrecord InMemoryBlockchain [blockchain-label transactions-atom accounts-atom]
  Blockchain
  ;; identifier
  (label [bk] blockchain-label)
  
  ;; account
  (import-account [bk account-id secret] nil)
  (create-account [bk]
    (let [secret (fxc/create-secret param/encryption blockchain-label)]
      {:account-id (:_id secret)
       :account-secret secret}))

  (get-address [bk account-id] nil)
  (get-balance [bk account-id] 0) ;; TODO: Will be implemented when driving out transaction code

  ;; transactions
  (list-transactions [bk account-id] ;; TODO
    )
  (get-transaction   [bk account-id txid] nil)
  (make-transaction  [bk from-account-id amount to-account-id secret] ;; TODO
    )

  ;; vouchers
  (create-voucher [bk account-id amount expiration secret])
  (redeem-voucher [bk account-id voucher]))

(defn create-in-memory-blockchain
  ([label] (create-in-memory-blockchain label (atom {}) (atom {})))
  
  ([label transactions-atom accounts-atom]
   (InMemoryBlockchain. label transactions-atom accounts-atom)))
