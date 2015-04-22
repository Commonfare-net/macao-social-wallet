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

(ns freecoin.nxt
;  (:use [freecoin.core])
  (:require
   [freecoin.secretshare :as ssss]
   [freecoin.utils :as util]

   [cheshire.core :refer :all :as jj]
   [clojure.pprint :as pp]
   [org.httpkit.client :as http])
  )

(def conf {:url "https://nxt.dyne.org:7876/nxt"
           :method :post             ; :post :put :head or other
           :user-agent "Freecoin 0.2"
           :headers {"X-header" "value"
                     "X-Api-Version" "2"}
           :keepalive 3000 ; Keep the TCP connection for 3000ms
           :timeout 1000 ; connection and reading timeout 1000ms
           :filter (org.httpkit.client/max-body-filter (* 1024 100))
           ;; reject if body is more than 100k
           :insecure? true
           ;; Need to contact a server with an untrust

           :max-redirects 10 ; Max redirects to follow
           ;; whether follow 301/302 redirects automatically, default
           ;; to true
           ;; :trace-redirects will contain the chain of the
           ;; redirections followed.
           :follow-redirects true
           })

;; synchronous
(defn call [arguments]
  {:pre [ (contains? arguments :query-params) ] }
  
  (let [{:keys [status headers body error] :as resp}
        (http/post (:url conf) (merge conf arguments))]
    (if (contains? @resp :error)
      (util/log! "Error" "nxt/call" (:error @resp)))
    ;; just debug for now
    ;;    (pp/pprint (:body @resp))
    (jj/parse-string (:body @resp) true) ; {:pretty true})
    )
  )


(defn getMyInfo[]
  (call {:query-params {"requestType" "getMyInfo"}})
  )
(defn getPeer [peer]
  "Request:
requestType is getPeer
peer is the IP address or domain name of the peer (plus optional port)
Response:
hallmark (S) is the hex string of the peer's hallmark, if it is defined
downloadedVolume (N) is the number of bytes downloaded by the peer
address (S) the IP address or DNS name of the peer
weight (N) is the peer's weight value
uploadedVolume (N) is the number of bytes uploaded by the peer
version (S) is the version of the software running on the peer
platform (S) is a string representing the peer's platform
lastUpdated (N) is the timestamp (in seconds since the genesis block) of the last peer status update
blacklisted (B) is true if the peer is blacklisted
blacklistingCause (S) is the cause of blacklising (if blacklisted is true)
announcedAddress (S) is the name that the peer announced to the network (could be a DNS name, IP address, or any other string)
application (S) is the name of the software application, typically NRS
state (N) defines the state of the peer: 0 for NON_CONNECTED, 1 for CONNECTED, or 2 for DISCONNECTED
shareAddress (B) is true if the address is allowed to be shared with other peers
requestProcessingTime (N) is the API request processing time (in millisec)"
  (call {:query-params {"requestType" "blacklistPeer"
                        "peer" (str peer)}})
  )

(defn getPeers []
  (call {:query-params {"requestType" "getPeers"}})
  )
(defn blacklistPeer [peer]
  (call {:query-params {"requestType" "blacklistPeer"
                        "peer" (str peer)}})
  )


(defn getAccountId[args]
  "*Request:*

* _requestType_ is _getAccountId_
* _secretPhrase_ is the secret passphrase of the account (optional)
* _publicKey_ is the public key of the account (optional if
_secretPhrase_ provided)

*Response:*

* _accountRS_ (S) is the Reed-Solomon address of the account
* _publicKey_ (S) is the public key of the account
* _requestProcessingTime_ (N) is the API request processing time (in
millisec)
* _account_ (S) is the account number"
  (call {:query-params
         { "requestType" "getAccountId"
           "secretPhrase" (:secret args) }})
  )

(defn getAccountPublicKey[args]
  "*Request:*

* _requestType_ is _getAccountPublicKey_
* _account_ is the accountRS representation

*Response:*

* _publicKey_ (S) is the 32-byte public key associated with the account,
returned as a hex string
* _requestProcessingTime_ (N) is the API request processing time (in
millisec)"
  (util/log! 'ACK 'getAccountPublicKey (:accountRS args))
  (call {:query-params
         { "requestType" "getAccountPublicKey"
           "account" (:accountRS args) }})
  )

(defn getBalance[account]
  "getBalance(accountid)
Request:
requestType is getGuaranteedBalance
account is an account ID
numberOfConfirmations is the minimum number of confirmations for a transaction to be included in the guaranteed balance (optional, if omitted or zero then minimally confirmed transactions are included)
Response:
guaranteedBalanceNQT (S) is the balance (in NQT) of the account with at least numberOfConfirmations confirmations
requestProcessingTime (N) is the API request processing time (in millisec)"
  (call {:query-params
         { "requestType" "getBalance"
           "account" (str account) }
         }))

(defn getAllCurrencies[first last]
  "Request:
requestType is getAllCurrencies
firstIndex is a zero-based index to the first currency to retrieve (optional)
lastIndex is a zero-based index to the last currency to retrieve (optional)
includeCounts is false to omit numberOf... fields (optional)
Response:
currencies (A) is an array of currency objects (refer to Get Currency for details)
requestProcessingTime (N) is the API request processing time (in millisec)"
  (call {:query-params { "requestType" "getAllCurrencies"
                         "firstIndex" (int first)
                         "lastIndex"  (int last) }})
  )

(defn getCurrency[code]
  "Request:
requestType is getCurrency
currency is the currency ID (optional)
code is the currency code (optional if currency provided)
includeCounts is false if numberOf... fields are to be omitted (optional)
Response:
initialSupply (S) is the initial currency supply (in QNT)
currentReservePerUnitNQT (S) is the minimum currency reserve (in NQT per QNT)
types (A) is an array of currency types, one or more of EXCHANGEABLE, CONTROLLABLE, RESERVABLE, CLAIMABLE, MINTABLE, NON_SHUFFLEABLE
code (S) is the currency code
creationHeight (N) is the blockchain height of the currency creation
minDifficulty (N) is the minimum difficulty for a mintable currency
numberOfTransfers (N) is the number of currency transfers
description (S) is the currency description
minReservePerUnitNQT (S) is the minimum currency reserve (in NQT per QNT) for a reservable currency
currentSupply (S) is the current currency supply (in QNT)
issuanceHeight (N) is the blockchain height of the currency issuance for a reservable currency
requestProcessingTime (N) is the API request processing time (in millisec)
type (N) is the currency type bitmask, from least to most significant bit: exchangeable, controllable, reservable, claimable, mintable, non-shuffleable
reserveSupply (S) is the reserve currency supply (in NQT) for a reservable currency
maxDifficulty (N) is the maximum difficulty for a mintable currency
accountRS (S) is the Reed-Solomon address of the issuing account
decimals (N) is the number of decimal places used by the currency
name (S) is the name of the currency
numberOfExchanges (N) is the number of currency exchanges
currency (S) is the currency ID
maxSupply (S) is the maximum currency supply (in QNT)
account (S) is the account ID of the currency issuer
algorithm (N) is the algorithm number for a mintable currency: 2 for SHA256, 3 for SHA3, 5 for Scrypt, 25 for Keccak25"
  (call {:query-params { "requestType" "getCurrency"
                         "code" (str code) }})
  )

(defn searchCurrencies [query]
  "Request:
requestType is searchCurrencies
query is a full text query on the currency field code in the standard Lucene syntax
firstIndex is a zero-based index to the first currency to retrieve (optional)
lastIndex is a zero-based index to the last currency to retrieve (optional)
includeCounts is false if the fields beginning with numberOf... are to be omitted (optional)
Response:
currencies (A) is an array of currency objects (refer to Get Currency for details)
  requestProcessingTime (N) is the API request processing time (in millisec)"
  (call {:query-params { "requestType" "searchCurrencies"
                         "query" (str query) }})
)
