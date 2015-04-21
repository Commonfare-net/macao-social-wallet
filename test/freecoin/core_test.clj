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

;; we save our secret here to be able to check
(def secret [(rand/create 16 3.1)
             (rand/create 16 3.1)])
;; creates a keys structure with all shares
(def skeys (ssss/create conf secret))
;; (pp/pprint skeys)

(pp/pprint (:key skeys))
;; print out public information to console
(pp/pprint (str (format "total %d quorum %d"
                        (:total conf) (:quorum conf)
                        )))
;; pretty print some more
(doseq [[k v] conf]
  (if-not
      (some #{k} '( :plain :prime :quorum :total))
    (pp/pprint [k v]))
  )

(println (format "Shannon entropy => %f - %f"
                 (:entropy-lo skeys)
                 (:entropy-hi skeys)
                 ))

(loop [x 0]
  (when (< x (:total conf))
    (pp/pprint
     [(:index (nth (:shares-lo skeys) x))
      (:share (nth (:shares-lo skeys) x))
      (:share (nth (:shares-hi skeys) x))
      ]
     )
    (recur (inc x)))
  )


(pp/pprint "Encoding using hashids (weak encryption)")
(loop [x 0]
  (when (< x (:total conf))
    (pp/pprint
     [(:index (nth (:shares-lo skeys) x))
      (str (hash/encode conf (:share (nth (:shares-lo skeys) x))))
      (str (hash/encode conf (:share (nth (:shares-hi skeys) x))))
      ]
     )
    (recur (inc x)))
  )

(fact "Testing secureshare number splitting"

      (def singlesec (rand/create 16 3.1))
      (fact "checking secret number after (combine (shuffle (split)))"
            (ssss/combine
             (shuffle
              (ssss/split conf (:integer singlesec))))
            => (:integer singlesec)
            )

      (fact "checking secret number combine with only 4 elements"
            (ssss/combine
             (shuffle
              (take-nth 2 (ssss/split conf (:integer singlesec)))))
            => (:integer singlesec)
            )

      (doseq [s (map :share (:shares-lo skeys))]
        (fact "check hashid number encoding (lo)"
              (hash/decode conf (str (hash/encode conf s)))
              => [ s ]
              ))
      (doseq [s (map :share (:shares-hi skeys))]
        (fact "check hashid number encoding (hi)"
              (hash/decode conf (str (hash/encode conf s)))
              => [ s ]
              ))

      )

(fact "Checking higher level key api"
      (fact "check (unlock (create))"
            (ssss/unlock conf skeys) => (:key skeys)
            )
      )
