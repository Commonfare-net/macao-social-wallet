;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349) 

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Gareth Rogers <grogers@thoughtworks.com>

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

(ns freecoin.core-test
  (:use midje.sweet)
  (:use [freecoin.core])
  (:import [java.math])

  (:require
;   [freecoin.core :refer :all]
   
   [freecoin.secretshare :as ssss]
   [freecoin.random :as rand]
   
   [hashids.core :as hash]
   [clojure.math.numeric-tower :refer :all]

   [clojure.pprint :as pp]
   )
  )

(def secret 
  {
   ;; 18 digits is an optimal size for Shamir's secret sharing
   ;; to match hashids maximum capacity. 3.1 is the Shannon
   ;; entropy measurement minimum to accept the number.
   :plain (biginteger (rand/create 16 3.1))

   :total 8
   :quorum 4

   :prime (ssss/prime4096)

   :description "test freecoin ssss"

   ;; this alphabet excludes ambiguous chars:
   ;; 1,0,I,O can be confused on some screens
   :alphabet "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
   
   ;; the salt should be a secret shared password
   ;; known to all possessors of a the key pieces
   :salt "La gatta sul tetto che scotta"
   })

(def hash_opts
  {
   ;; this alphabet excludes ambiguous chars:
   ;; 1,0,I,O can be confused on some screens
   :alphabet "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

   ;; the salt should be a secret shared password
   ;; known to all possessors of a the key pieces
   :salt "La gatta sul tetto che scotta"
   })

(def secret-conf (ssss/secret-conf
                  (:total secret)
                  (:quorum secret)
                  (:prime secret) ; (ssss/prime4096)
                  (:description secret))) ; "midje testing secushare"))

;;(def secret-conf (ssss/secret-conf secret))
(def secret-pieces
   (ssss/split secret-conf (:plain secret)))
;;  (ssss/split secret)


;; (println (format "plain secret: %d" (:plain secret) ))
;; (println (format "proc  secret: %d" (ssss/combine secret-pieces)))
(println (format "Shannon entropy => %s"
                 (rand/entropy (format "%d" (:plain secret) ))))

(def first-secret (first secret-pieces))

;; (println "first piece of the secret:")
;; (clojure.pprint/pprint first-secret)

(pp/pprint
 (map :share secret-pieces))

(def first-share (:share first-secret))


;; print out public information to console
(pp/pprint (str (format "total %d quorum %d"
                        (:total secret)
                        (:quorum secret)
                        )))
(doseq [[k v] secret]
  (if-not
      (some #{k} '( :plain :prime :quorum :total))
    (pp/pprint [k v]))
  )

(pp/pprint "Encoding using hashids (weak encryption)")
(doseq [s secret-pieces]
  (pp/pprint (str (hash/encode hash_opts (:share s))))
  )



;; checks are limited only to the first share right now

(fact "Testing secureshare number splitting"

      (fact "checking secret number after (combine (shuffle (split)))"
             (ssss/combine
              (shuffle
               (ssss/split secret-conf
                           (:plain secret))))
              => (:plain secret)
              )

      (doseq [s (map :share secret-pieces)]
        (fact "check hashid number encoding"
              (hash/decode hash_opts (str (hash/encode hash_opts s)))
              => [ s ]
              ))
      )
