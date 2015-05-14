;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation

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


(ns freecoin.fxc
  (:use midje.sweet)
  (:require
   [freecoin.secretshare :as ssss]
   [freecoin.random :as rand]
   [freecoin.utils :as util]
   [freecoin.db :as db]

   [clojure.string :as str]
   [clojure.pprint :as pp]
   
   )
  )

(declare render-slice)
(declare extract-ahal)
(declare extract-int)

(defn create-secret
  "Takes a configuration and optionally two integers, creates a wallet
  address, returned as a string"

  ([conf]
   (let [ah (:integer (rand/create (:length conf) (:entropy conf)))
         al (:integer (rand/create (:length conf) (:entropy conf)))]
     (create-secret conf ah al)
     ))
  
  ([conf hi lo]
   {:pre  [(contains? conf :version)
           (contains? conf :length)
           (contains? conf :entropy)]
    :post [(= (:total conf) (count (:slices %)))]}

   (let [ah  (ssss/shamir-split ssss/config hi)
         al  (ssss/shamir-split ssss/config lo)]

     ;; wallet rendering
     ;; slices: collection of rendered string slices for ssss
     {:_id (format "%s_%s_FXC_%s" (:prefix conf)
                   (get-in ah [:header :_id])
                   (get-in al [:header :_id]))
      :config conf

      :slices (loop [lah (first (:shares ah))
                     lal (first (:shares al))
                     res []
                     c 1]
                (if (< c (count (:shares ah)))
                  (recur (nth (:shares ah) c)
                         (nth (:shares al) c)
                         (merge res (render-slice conf lah lal c))
                         (inc c))
                  (merge res (render-slice conf lah lal c))))

      :cookie (render-slice conf
                            (first (:shares ah))
                            (first (:shares al)) 1)
      }
     ))
  )

(defn extract-quorum [conf secret slice]
  {:pre  [(contains? conf :quorum)]}
;;   :post [(= (count (get-in % (:ah :shares))) (:quorum conf))]}

  "Takes a config, a secret and a slice and tries to combine the secret and the slice in a collection of integers ready for shamir-combine. Returns a map {:ah :al} with the collections."

  ;; reconstruct a collection of slices
  (loop [cah [(extract-int conf slice "ah")]
         cal [(extract-int conf slice "al")]
         c 1]

    (util/log! 'ACK 'extract-quorum (str "ah:    " [ c cah]))
    (util/log! 'ACK 'extract-quorum (str "al:    " [ c cal]))
    ;; (util/log! 'ACK 'extract-quorum (str "cookie:" 
    ;;                                      [c (extract-int conf (:cookie secret) "ah")
    ;;                                       (extract-int conf (:cookie secret) "al")]
    ;;                                      ))

    (if (< (count cah) (:quorum conf))

      (recur
       (conj cah (extract-int conf (nth (:slices secret) c) "ah"))
       (conj cal (extract-int conf (nth (:slices secret) c) "al"))
       (inc c))

      ;; return
      {:ah {:header {:_id (:_id secret)
                     :quorum (int (:quorum conf))
                     :total (int (:total conf))
                     :prime (:prime conf)
                     :description (:description conf)}
            :shares (map biginteger (conj cah (extract-int conf (nth (:slices secret) c) "ah")))}
       :al {:header {:_id (:_id secret)
                     :quorum (int (:quorum conf))
                     :total (int (:total conf))
                     :prime (:prime conf)
                     :description (:description conf)}
            :shares (map biginteger (conj cal (extract-int conf (nth (:slices secret) c) "al")))}
       }

      ))
  )
  

;; (defn unlock-secret [conf secret slice]
;;   "Takes a config, id of a secret and a slice of it. Returns a secret passphrase to NXT"
;;   (extract-quorum conf secret slice)
;;   ;; (if (< (+ 1 (count (:slices secret))) (:quorum conf))

;;   ;;   ;; not enough slices to get the secret
;;   ;;   nil

    
;;     )
  
 

(defn render-slice [conf ah al idx]
   (format "%s_%s_FXC_%s_%d" (:prefix conf)
           (ssss/hash-encode-num conf ah)
           (ssss/hash-encode-num conf al)
           idx)
   )

(defn extract-ahal [conf addr ah-or-al]
  "extract the lower integer in an fxc address"
  (try
    (let [toks (str/split (util/trunc addr 128) #"_")]
      (if-not (= (subs (first toks) 0 4) "FXC1")
        (throw (Exception.
                (format "Invalid FXC address: %s" (first toks))))
        (nth toks (case ah-or-al "al" 3 "ah" 1 0))
        ;; TODO: check that only valid characters in alphabet are present
        ))

    (catch Exception e
      (let [error (.getMessage e)]
        (util/log! 'ERR 'fxc/extract-lo error)
        ))

    (finally)
    )
  )

(defn extract-int [conf addr ah-or-al]
  (ssss/hash-decode-str conf
   (extract-ahal conf addr ah-or-al))
  )
