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
  (:require
   [freecoin.secretshare :as ssss]
   [freecoin.params :as param]
   [freecoin.utils :as util]
   [freecoin.auth :as auth]

   [liberator.core :refer [resource defresource]]


   [cheshire.core :refer :all :as cheshire]
   [org.httpkit.client :as http]
   [json-html.core :as present]
   [hiccup.page :as page]

   )
  )

;; operations always needing accountId
(def account-ops ["getAccountId"
                  "getAccount"
                  "getAccountBlockCount"
                  "getAccountBlockIds"
                  "getAccountBlocks"
                  "getAccountLessors"
                  "getAccountPublicKey"
                  "getAccountTransactionIds"
                  "getAccountTransactions"
                  "getBalance"
                  "getGuaranteedBalance"
                  "getUnconfirmedTransactionIds"
                  "getUnconfirmedTransactions"
                  "getAccountAssetCount"
                  "getAccountAssets"
                  "getAccountCurrentBidOrderIds"
                  "getAccountCurrentAskOrderIds"
                  "getAccountCurrentBidOrders"
                  "getAccountCurrentAskOrders"
                  "getAccountCurrencies"
                  "getAccountCurrencyCount"])

(def currency-ops ["getCurrency" ;; optional code
                   "getCurrencyAccountCount"
                   "getCurrencyAccounts"
                   "getCurrencyFounders"
                   "getcurrencyTransfers" ;; optional id
                   "getExchanges" ;; optional id
                   "canDeleteCurrency" ;; id
                   "deleteCurrency"
                   "getAccountCurrencies" ;; id
                   "getAccountExchangeRequests" ;; id
                   "getBuyOffers" "getSellOffers" ;; optional id
                   "getCurrencyPhasedTransactions"])

;; synchronous
(defn call [arguments]
  {:pre [ (contains? arguments :query-params) ] }

  (let [{:keys [status headers body error] :as resp}
        (http/post (:url param/nxt) (merge param/nxt arguments))]
    (if (contains? @resp :errorCode)
      (:errorDescription @resp))
    (cheshire/parse-string (:body @resp) true) ; {:pretty true})
    )
  )

;; ring resource for direct wiring
(defresource api [request command]
;;{:pre (string? command)}

  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :authorized? (fn [ctx] {:apikey (auth/get-apikey request)})

  :handle-ok (fn [ctx]
               (let [apikey (:apikey ctx)]
                 (if (empty? apikey) nil
                     (let [cmd (val (first command))]

                       (if (some #{cmd} account-ops)

                         ;; is an account api command
                         (let [account (:account (auth/get-wallet request apikey))]
                           (page/html5
                            [:head [:style (-> "json.human.css" clojure.java.io/resource slurp)]]
                            (present/edn->html (call {:query-params
                                                      (conj {"account" account} command)}))
                            ))

                         ;; else
                         (page/html5
                          [:head [:style (-> "json.human.css" clojure.java.io/resource slurp)]]
                          (present/edn->html (call {:query-params command}))
                          )
                         )))))
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
