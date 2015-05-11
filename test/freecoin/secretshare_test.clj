;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Based on Secret Share Java implementation by Tim Tiemens
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

(ns freecoin.secretshare-test
  (:use midje.sweet)
  (:require
   [freecoin.secretshare :as ssss]
   [freecoin.random :as rand]
   [freecoin.utils :as util]
   )
  )

(fact "Create Shamir's secret split into shares"
  
  (type  (ssss/shamir-create-new ssss/config)) => clojure.lang.PersistentArrayMap
  (count (:shares (ssss/shamir-create-new ssss/config))) => (:total ssss/config)
  ((ssss/get-prime (:prime (:header (ssss/shamir-create-new ssss/config))))) => ((ssss/get-prime (:prime ssss/config)))
  (fact "Shamir is not deterministic"
    (let [r (rand/create (:length ssss/config) (:entropy ssss/config))]
      (ssss/shamir-create-new ssss/config r) =not=> (ssss/shamir-create-new ssss/config r)
;;      (util/log! 'ACK 'shamir-create-new (ssss/shamir-create-new ssss/config r))
      )
    )
  )


(fact "Combine Shamir's shares into the secret"
;;  (util/log! 'FACT 'shamir-combine 'START)
  (let [secnum (rand/create (:length ssss/config) (:entropy ssss/config))
        secret (ssss/shamir-create-new ssss/config secnum)]
    (ssss/shamir-combine secret) => (:integer secnum)
    )
;;  (util/log! 'FACT 'shamir-combine 'END)
  )


(fact "Hashid codec"
;;  (util/log! 'FACT 'hashid-codec 'START)
  (let [r (rand/create (:length ssss/config) (:entropy ssss/config))
        secret (ssss/shamir-create-new ssss/config r)
        hashes (ssss/hash-encode-secret ssss/config secret)
        hasdec (ssss/hash-decode-hashes ssss/config hashes)
        secnum (first (:shares secret))
        ]
    hasdec => (:shares secret)
    
    (ssss/hash-decode-str ssss/config (ssss/hash-encode-num ssss/config secnum)) => secnum
    (let [enc (ssss/hash-encode-num ssss/config secnum)
          dec (ssss/hash-decode-str ssss/config enc)
          rec (ssss/hash-encode-num ssss/config dec)]
      secnum => dec
      enc => rec
      ;;      (util/log! 'ACK 'hash-codec [(type enc) enc (type dec) dec (type rec) rec])
      )
    )
  
;;  (util/log! 'FACT 'hashid-codec 'END)
  )

(fact "Tuple generator"
;;  (util/log! 'ACK 'new-tuple (ssss/new-tuple ssss/config))
  )
