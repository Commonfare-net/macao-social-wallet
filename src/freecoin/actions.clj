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

(ns freecoin.actions
  (:require
   [clojure.pprint :as pp]
   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [cheshire.core :refer :all :as jj]

   [freecoin.secretshare :as ssss]
   [freecoin.pages :as pages]
   [freecoin.utils :as util]
   [freecoin.nxt :as nxt]
   )
  )

;; this is perhaps a good place to plug in more backends in the future
;; we only use NXT for now (the monetary system API)

(defn create-account [auth]
  (let [acct (nxt/getAccountId auth)]
    (if (contains? acct :errorCode)
      (util/log! 'Error 'create-account/getAccountId (:errorDescription acct))      
      (util/log! 'ACK 'create-account/getAccountId (:accountRS acct))
      )
    ;; should return error code 5 (unknown account) to be sure it doesn't exist already
    (let [pbk (nxt/getAccountPublicKey acct)]
      (if (= 5 (:errorCode pbk))
        (util/log! 'Success 'create-account "no collision detected")
        (util/log! 'Error 'create-account/getAccountPublicKey (:errorDescription pbk))
        )
      )
    ;; return
    acct
    )
  )

(defn get-balance [auth]
  (pp/pprint ["get-balance" auth])
  (nxt/getBalance (:_id auth))
  )
