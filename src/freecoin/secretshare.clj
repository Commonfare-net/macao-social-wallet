;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Based on Secret Share Java implementation by Tim Tiemens
;; Copyright (C) 2015 Denis Roio <jaromil@dyne.org>

;; Shamir's Secret Sharing algorithm was invented by Adi Shamir
;; Shamir, Adi (1979), "How to share a secret", Communications of the ACM 22 (11): 612–613
;; Knuth, D. E. (1997), The Art of Computer Programming, II: Seminumerical Algorithms: 505

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

(ns freecoin.secretshare
  (:gen-class)
  (:import [com.tiemens.secretshare.engine SecretShare])
  (:require [freecoin.utils :as util]
            [hashids.core :as hash]))

(defn prime384 []
  (SecretShare/getPrimeUsedFor384bitSecretPayload))

(defn prime192 []
  (SecretShare/getPrimeUsedFor192bitSecretPayload))

(defn prime4096 []
  (SecretShare/getPrimeUsedFor4096bigSecretPayload))

(defn get-prime [sym]
  (ns-resolve *ns* (symbol (str "freecoin.secretshare/" sym))))


(defn shamir-set-header [head]
  "Takes an header and sets it into Tiemen's structure"
  (let [res
        (SecretShare.
         (com.tiemens.secretshare.engine.SecretShare$PublicInfo.
          (int (:total head))
          (:quorum head)
          ((get-prime (:prime head)))
          (:description head)
          )
         )]
    res
    )
  )

(defn shamir-get-header [share]
  "Takes Tiemen's share and extracts a header"
  (let [pi (.getPublicInfo share)]
    {:_id (.getUuid pi)
     :quorum (.getK pi)
     :total (.getN pi)
     :prime (condp = (.getPrimeModulus pi)
              (prime192) 'prime192
              (prime384) 'prime384
              (prime4096) 'prime4096
              (str "UNKNOWN"))

     :description (.getDescription pi)
     })
  )

(defn shamir-get-shares [si]
  "Takes Tiemen's share and extract a collection of shares"
  (map (fn [_] (.getShare _)) si))

(defn shamir-split
  "split an integer into shares according to conf
  return a structure { :header { :quorum :total :prime :description}
                       :shares [ integer vector of :total length] }"
  [conf secnum]
  (let [si (.getShareInfos (.split (shamir-set-header conf) secnum))
        header (shamir-get-header (first si))
        shares (shamir-get-shares si)]

    {
     :header header
     :shares (map biginteger shares)
     }
    )
  )

(defn shamir-combine [secret]
  {:pre [(contains? secret :header)
         (contains? secret :shares)
         (coll? (:shares secret))]
   :post [(integer? %)]}
  "Takes a secret (header and collection of integers) and returns the
  unlocked big integer"

  (let [header (:header secret)
        shares (:shares secret)]
    ;; (util/log! "ACK" 'shamir-combine [ header shares ])
    (loop [s (first shares)
           res []
           c 1]

      ;; TODO: check off-by-one on this one
      
      (if (< c (count shares))
        (recur (nth shares c)
               (conj res (com.tiemens.secretshare.engine.SecretShare$ShareInfo.
                           c  s (com.tiemens.secretshare.engine.SecretShare$PublicInfo.
                                 (:total header) (:quorum header)
                                 ((get-prime (:prime header)))
                                 (:description header))
                           ))
               (inc c))
        ;; return
        (.getSecret (.combine (shamir-set-header header) res))
        ))
    )
  )

(defn hash-encode-num [conf num]
  {:pre  [(integer? num)]
   :post [(string? %)]}
  (str (hash/encode conf num))
  )

(defn hash-decode-str [conf str]
  {:pre  [(string? str)]
   :post [(integer? %)]}
  ;; (util/log! 'ACK 'decoding-type (type str))
  ;; (util/log! 'ACK 'decoded-type (type (hash/decode conf str)))
  (biginteger (first (hash/decode conf str)))
  )

(defn hash-encode-secret [conf secret]
  {:pre [(contains? secret :header)
         (contains? conf :alphabet)
         (contains? conf :salt)
         (contains? secret :shares)
         (= (count (:shares secret)) (:total (:header secret)))]
   :post (= (count (:shares secret)) (count %))}
  (loop [c 1
         s (first (:shares secret))
         res [] ]

    (if (< c (count (:shares secret)))
      (recur (inc c) (biginteger (nth (:shares secret) c))
             (merge res (hash-encode-num conf s)))
      ;; return
      (merge res (hash-encode-num conf s)))
    )
  )

(defn hash-decode-hashes [conf hashes]
  {:pre [(contains? conf :salt)
         (coll? hashes)
         (string? (first hashes))]
   :post [(coll? %)
          (integer? (first %))
          (= (count hashes) (count %))]}

  (loop [c 1
         s (first hashes)
         res [] ]

    (if (< c (count hashes))
      (recur (inc c) (str (nth hashes c))
             (merge res (biginteger (hash-decode-str conf s))))
      ;; return
      (merge res (biginteger (hash-decode-str conf s))))
    )
  )

