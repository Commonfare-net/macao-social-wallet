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

(ns freecoin.fxc-test
  (:use midje.sweet)
  (:require
   [freecoin.fxc :as fxc]
   [freecoin.random :as rand]
   [freecoin.secretshare :as ssss]

   [clojure.pprint :as pp]

   )
  )


(fact "Secret FXC codec"

  ;; manual creation of an nxt address to intercept it in clear
  (def fake {:ah (:integer (rand/create (:length ssss/config)
                                        (:entropy ssss/config)))
             :al (:integer (rand/create (:length ssss/config)
                                        (:entropy ssss/config)))
             })
  (def nxtpass (fxc/render-slice ssss/config (:ah fake) (:al fake)))
  (def secret (fxc/create-secret ssss/config (:ah fake) (:al fake)))
  
  (pp/pprint nxtpass)
  
  (pp/pprint secret)
  
  (fact "cookie is first slice" (first (:slices secret)) => (:cookie secret))

  (fact "reversable extraction / hash / conversion for AH"
    (fxc/extract-ahal ssss/config (:cookie secret) "ah")
    =>
    (ssss/hash-encode-num ssss/config
                          (fxc/extract-int ssss/config (first (:slices secret)) "ah"))
    )
  (fact "reversable extraction / hash / conversion for AL"
    (fxc/extract-ahal ssss/config (:cookie secret) "al")
    =>
    (ssss/hash-encode-num ssss/config
                          (fxc/extract-int ssss/config (first (:slices secret)) "al"))
    )

  (fact "unlocking secret"
    (let [quorum (fxc/extract-quorum ssss/config secret (:cookie secret))
          ah (ssss/shamir-combine (:ah quorum))
          al (ssss/shamir-combine (:al quorum))]
      ah => (:ah fake)
      al => (:al fake)
      (fxc/render-slice ssss/config ah al) => nxtpass
      )
    )
)
