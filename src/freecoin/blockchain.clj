;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Arjan Scherpenisse <arjan@scherpenisse.net>
;; Amy Welch <awelch@thoughtworks.com>

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
            [freecoin.utils :as util]
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
  (list-transactions [bk] [bk account-id])
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

(defn- normalize-transactions [list]
  (reverse
     (sort-by :timestamp
              (map (fn [transaction]
                     (assoc transaction :amount (util/long->bigdecimal (:amount transaction))))
                   list
                   ))))

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
                                                 [{"$match" {:to-id account-id}}
                                                  {"$group" {:_id "$to-id"
                                                             :total {"$sum" "$amount"}}}]))
          sent-map  (first (storage/aggregate db "transactions"
                                              [{"$match" {:from-id account-id}}
                                               {"$group" {:_id "$from-id"
                                                          :total {"$sum" "$amount"}}}]))
          received (if (nil? received-map) 0 (:total received-map))
          sent      (if (nil? sent-map) 0 (:total sent-map))]
      (util/long->bigdecimal (- received sent))))

  (list-transactions [bk]
    (normalize-transactions
     (storage/find-by-key db "transactions" {:blockchain "STUB"})))

  (list-transactions [bk account-id]
    (normalize-transactions
     (concat
      (storage/find-by-key db "transactions" {:from-id account-id})
      (storage/find-by-key db "transactions" {:to-id account-id}))))

  (get-transaction   [bk account-id txid] nil)

  (make-transaction  [bk from-account-id amount to-account-id secret]
    (let [now (time/format (time/now))
          transaction {:_id (str now "-" from-account-id)
                       :blockchain "STUB"
                       :timestamp now
                       :from-id from-account-id
                       :to-id to-account-id
                       :amount (util/bigdecimal->long amount)}]
      ;; TODO: Keep track of accounts to verify validity of from- and
      ;; to- accounts
      (storage/insert db "transactions" transaction)))

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
  (get-balance [bk account-id]
    (let [all-transactions (vals @transactions-atom)
          total-withdrawn (->> all-transactions
                               (filter (comp (partial = account-id) :from-account-id))
                               (map :amount)
                               (reduce +))
          total-deposited (->> all-transactions
                               (filter (comp (partial = account-id) :to-account-id))
                               (map :amount)
                               (reduce +))]
      (- total-deposited total-withdrawn)))

  ;; transactions
  (list-transactions [bk account-id] (vals @transactions-atom))
  (list-transactions [bk] (vals @transactions-atom))
  (get-transaction   [bk account-id txid] nil)
  (make-transaction  [bk from-account-id amount to-account-id secret]
    ;; to make tests possible the timestamp here is generated starting from
    ;; the 1 december 2015 plus a number of days that equals the amount
    (let [now (time/format (time/add-days (time/datetime 2015 12 1) amount))
          transaction {:transaction-id (str now "-" from-account-id)
                       :blockchain "INMEMORYBLOCKCHAIN"
                       :timestamp now
                       :from-id from-account-id
                       :to-id to-account-id
                       :amount amount}]
      (swap! transactions-atom assoc (:transaction-id transaction) transaction)
      transaction))

  ;; vouchers
  (create-voucher [bk account-id amount expiration secret])
  (redeem-voucher [bk account-id voucher]))

(defn create-in-memory-blockchain
  ([label] (create-in-memory-blockchain label (atom {}) (atom {})))

  ([label transactions-atom accounts-atom]
   (InMemoryBlockchain. label transactions-atom accounts-atom)))
