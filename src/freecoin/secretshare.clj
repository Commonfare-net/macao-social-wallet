;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349) 

;; Wrapper based on Secret Share Java implementation by Tim Tiemens
;; Copyright (C) 2015 Denis Roio <jaromil@dyne.org>

;; Shamir's Secret Sharing algorithm was invented by Adi Shamir
;; Shamir, Adi (1979), "How to share a secret", Communications of the ACM 22 (11): 612–613
;; Knuth, D. E. (1997), The Art of Computer Programming, II: Seminumerical Algorithms: 505

;; This library is free software; you can redistribute it and/or
;; modify it under the terms of the GNU Lesser General Public
;; License as published by the Free Software Foundation; either
;; version 3 of the License, or (at your option) any later version.

;; This library is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
;; Lesser General Public License for more details.

;; You should have received a copy of the GNU Lesser General Public
;; License along with this library; if not, write to the Free Software
;; Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

(ns freecoin.secretshare
  (:gen-class)
  (:import
   [com.tiemens.secretshare.engine SecretShare]
   [java.math]
   )
  )

(defn prime384 []
  (SecretShare/getPrimeUsedFor384bitSecretPayload))

(defn prime192 []
  (SecretShare/getPrimeUsedFor192bitSecretPayload))

(defn prime4096 []
  (SecretShare/getPrimeUsedFor4096bigSecretPayload))

(defn secret-conf

  ([ n k m description]
   (com.tiemens.secretshare.engine.SecretShare$PublicInfo.
    (int n) k m description)
   )

  ([ sec ]
   (com.tiemens.secretshare.engine.SecretShare$PublicInfo. 
    (int (:total sec))
    (:quorum sec)
    (:prime sec)
    (:description sec)
    )
   )

  )

(defn ss
  ([pi]
   (SecretShare. pi)))

(defn conf2map [si]
  "Convert a secretshare configuration structure into a clojure map"
  (let [pi (.getPublicInfo si)]
    {:index (.getIndex si)
     :share (.getShare si)
     :quorum (.getN pi)
     :total (.getK pi)
     :prime (.getPrimeModulus pi)
     :uuid (.getUuid pi)
     :description (.getDescription pi)}))

(defn map2conf [m]
  "Convert a clojure map into a secretshare configuration structure"
  (com.tiemens.secretshare.engine.SecretShare$ShareInfo.
   (:index m) (:share m)
   (com.tiemens.secretshare.engine.SecretShare$PublicInfo.
    (:quorum m)
    (:total m)
    (:prime m)
    (:description m))))

(defn split
  ([conf data]
   (map conf2map
        (.getShareInfos
         (.split (ss conf) data))))

  ([conf]
   (map conf2map
        (.getShareInfos
         (.split 
          (secret-conf conf)          
          (:plain conf)))))
  )


(defn combine
  ([shares]
   (let [shares (map map2conf shares)
         pi     (.getPublicInfo (first shares))]
     (.getSecret
      (.combine (ss pi) (vec shares)))))
  )
