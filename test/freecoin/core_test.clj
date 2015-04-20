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

(def conf
  ;; see secretshare/config for full options
  (merge ssss/config
         {
          :total 8
          :quorum 4
          :description "test freecoin ssss"
          }))

; (def secret (ssss/create conf))
;; (pp/pprint (:keys secret))

(def secret-pieces
   (ssss/split conf secret))
(pp/pprint secret-pieces)

(println (format "Shannon entropy => %s"
                 (rand/entropy (format "%d" (:plain conf) ))))

(def first-secret (first secret-pieces))

(pp/pprint
 (map :share secret-pieces))

(def first-share (:share first-secret))


;; print out public information to console
(pp/pprint (str (format "total %d quorum %d"
                        (:total conf) (:quorum conf)
                        )))
(doseq [[k v] conf]
  (if-not
      (some #{k} '( :plain :prime :quorum :total))
    (pp/pprint [k v]))
  )

(pp/pprint "Encoding using hashids (weak encryption)")
(doseq [s secret-pieces]
  (pp/pprint (str (hash/encode conf (:share s))))
  )

(fact "Testing secureshare number splitting"

      (fact "checking secret number after (combine (shuffle (split)))"
             (ssss/combine
              (shuffle
               (ssss/split conf secret)))
              => secret
              )

      (doseq [s (map :share secret-pieces)]
        (fact "check hashid number encoding"
              (hash/decode conf (str (hash/encode conf s)))
              => [ s ]
              ))
      )
