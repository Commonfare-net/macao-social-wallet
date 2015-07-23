;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

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

(ns freecoin.confirmations
  (:require
   [clojure.string :as str]

   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]

   [simple-time.core :as time]

   [freecoin.blockchain :as blockchain]
   [freecoin.participants :as participant]
   [freecoin.secretshare :as ssss]
   [freecoin.storage :as storage]
   [freecoin.params :as params]
   [freecoin.random :as rand]
   [freecoin.views :as views]
   [freecoin.utils :as utils]
   [freecoin.auth :as auth]

   )
  )

;; Confirmation's data structure
;; {:_id     code
;;  :action  name
;;  :data    collection}
;; where the last :data collection is identified by the :action field


(def confirmation-form-spec
  {:fields [{:name :id :type :text}]
   :validations [[:required [:id]]]}
  )

(defn create [db action data]
  (let [code (ssss/hash-encode-num
              params/encryption (:integer (rand/create 16)))
        stored (storage/insert
                db "confirmations" {:_id code
                                    :action action
                                    :data data})]
    ;; TODO depending from the type of confirmation configured
    ;; here should send an email or simply redirect
    (if (nil? stored)
      {:status 500
       :body "cannot create confirmation"}
      {:status 200
       :location (str "/confirmations/" (:_id stored))}
      )
    ))

(defresource get-confirm-form [request code]
  :service-available?
  {::db (get-in request [:config :db-connection])
   ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :allowed? (fn [ctx]
              (let [found (storage/find-by-id
                           (::db ctx) "confirmations" code)]

                (if (contains? found :error)
                  [false {::error (:error found)}]

                  (if (empty? found)
                    [false {::error "confirmation not found"}]
                    ;; here fill in the content of the confirmation
                    ;; {:_id     code
                    ;;  :action  name
                    ;;  :data    collection}
                    [true {::user-data found}]))
                ))

  :handle-forbidden (fn [ctx]
                      (ring-response {:status 404
                                      :body (::error ctx)}))

  :handle-ok (fn [ctx] (views/confirm-button (::user-data ctx)))
  )

(defresource execute [request]
  :service-available?
  {::db (get-in request [:config :db-connection])
   ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:post]
  :available-media-types ["application/json"
                          "application/x-www-form-urlencoded"]

  ;; :authorized?           (:result  (auth/check request))
  ;; :handle-unauthorized   (:problem (auth/check request))

  ;; SEC TODO: this is an open endpoint where callers can trigger database
  ;; queries even if not authenticated. It must be protected with
  ;; throttling and blacklisting
  :allowed?
  (fn [ctx]
    (let [{:keys [status data problems]}
          (views/parse-hybrid-form
           request confirmation-form-spec
           (::content-type ctx))]
      (case status
        :ok ;; parse query result
        (let [found (storage/find-by-id
                     (::db ctx) "confirmations"
                     (:id data))]
          (if (contains? found :error)
            [false {::problems (:error found)}]
            (if (empty? found)
              [false {::user-data data
                      ::problems "confirmation not found"
                      :representation
                      {:media-type
                       (get views/response-representation
                            (::content-type ctx))}}]

              [true {::user-data found}])))

        ;; form parsing problems
        :error
        [false {::user-data data
                ::problems (map pr-str problems)
                :representation
                {:media-type
                 (get views/response-representation
                      (::content-type ctx))}}]
        )))

  :handle-forbidden (fn [ctx]
                      (ring-response {:status 404
                                      :body (::problems ctx)}))

  :handle-created
  (fn [ctx]
    (let [confirm (::user-data ctx)
          action (:action confirm)
          data (:data confirm)
          db (::db ctx)]
      (case action

        ;; process signin confirmations
        "signin"
        (let [new-wallet
              (blockchain/create-account
               (blockchain/new-stub db)
               {:_id ""
                :name  (:name data)
                :email (:email data)
                :public-key nil
                :private-key nil
                :blockchains {}
                :blockchain-secrets {}})
              secret (get-in new-wallet [:blockchain-secrets :STUB])
              secret-without-cookie (dissoc secret :cookie)
              cookie-data (str/join "::" [(:cookie secret) (:_id secret)])]
          (utils/log! ::ACK 'signin cookie-data)

          ;; TODO consistent error reporting
          (if (contains? new-wallet :problem)
            ;; TODO consistent error reporting
            (utils/log! (::error new-wallet))
            (do
              ;; insert in the wallet database, use
              ;; the shamir's generated UID as _id
              (storage/insert db "wallets"
                              (assoc new-wallet :_id (:_id secret) ))
              ;; return the apikey cookie
              (ring-response {:session {:cookie-data cookie-data}
                              :apikey cookie-data})
              ;; TODO: give PINs for backup

              ;; local backup approach: use pub/priv key
              ;; pair to encrypt a message
              ;; if the message is brought back with a
              ;; name, try the secret key. If works,
              ;; then restore the account.

              ;; blockchain backup approach:
              ;; use the PIN to unlock shamir's secret,
              ;; see if it produces a valid passphrase
              ;; that is recognized by the
              ;; blockchain. If yes, restore the account.
              )))

        ;; process transaction confirmations
        "transaction"
        (let [wallet (auth/get-wallet request)
              tr (blockchain/make-transaction
                  (blockchain/new-stub db) wallet
                  (:amount data) (:recipient data)
                  nil)]
          ;; TODO: return a well formatted page
          tr)

        (ring-response
         {:status 404
          :body (pr-str "unknown action" action)}))
      ;; TODO:delete on success
      ))
  )
