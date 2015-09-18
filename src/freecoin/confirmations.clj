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
  (:require [clojure.string :as str]
            [liberator.core :as lc]
            [liberator.representation :as lr]
            [simple-time.core :as time]
            [freecoin.blockchain :as blockchain]
            [freecoin.secretshare :as ssss]
            [freecoin.storage :as storage]
            [freecoin.params :as params]
            [freecoin.random :as rand]
            [freecoin.views :as views]
            [freecoin.utils :as utils]
            [freecoin.auth :as auth]))

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
                (:db db) "confirmations" {:_id code
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

(lc/defresource get-confirm-form [request code]
  :service-available?
  {::db (get-in request [:config :db-connection])
   ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :allowed? (fn [ctx]
              (let [found (storage/find-by-id
                           (:db (::db ctx)) "confirmations" code)]

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
                      (lr/ring-response {:status 404
                                         :body (::error ctx)}))

  :handle-ok (fn [ctx] (views/confirm-button (::user-data ctx)))
  )

(defn empty-wallet [name email]
;;     [_id
;;      name
;;      email
;;      public-key
;;      private-key
;;      blockchains
;;      blockchain-keys

  {:_id ""            ;; unique id
   :name  name        ;; identifier, case insensitive, space counts
   :email email       ;; verified email account
   :info nil          ;; misc information text on the account
   :creation-date nil ;; date on which the wallet was created
   :last-login nil    ;; last time this participant logged in succesfully
   :last-login-ip nil ;; connection ip address of the last succesful login
   :failed-logins nil ;; how many consecutive failed logins were attempted
   :public-key nil    ;; public asymmetric key for off-the-blockchain encryption
   :private-key nil   ;; private asymmetric key for off-the-blockchain encryption
   :blockchains {}       ;; list of blockchains and public account ids
   :blockchain-keys {}}) ;; list of keys for private blockchain operations

(lc/defresource execute [request]
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
                     (:db (::db ctx)) "confirmations"
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
                      (lr/ring-response {:status 404
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
        (let [{:keys [account-id account-secret]} (blockchain/create-account (blockchain/new-stub (:db db)))
              secret-without-cookie (dissoc account-secret :cookie)
              cookie-data (str/join "::" [(:cookie account-secret) account-id])]
          (utils/log! ::ACK 'signin cookie-data)

          ;; TODO consistent error reporting
          #_(if false ;(contains? new-account :problem) ;; WIP DM 20150816 - refactoring blockchain
            ;; TODO consistent error reporting
            (utils/log! (::error new-account))
            (do
              ;; insert in the wallet database, use
              ;; the shamir's generated UID as _id
              (storage/insert (:db db) "wallets"
                              (assoc new-account :_id (:_id secret)))
              ;; return the apikey cookie
              (lr/ring-response {:session {:cookie-data cookie-data}
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
              ))
          (lr/ring-response {:session {:cookie-data cookie-data}
                             :apikey cookie-data}))

        ;; process transaction confirmations
        ;; WIP: Removing knowledge of wallet internals from blockchain
        ;; --- need account ids, not wallets.  This is currently
        ;; broken.
        "transaction"
        (let [wallet (auth/get-wallet request)
              from-account-id (get-in wallet [:blockchain :STUB])
              to-account-id "NOT-IMPLEMENTED"
              tr (blockchain/make-transaction
                  (blockchain/new-stub (:db db)) wallet
                  (:amount data)
                  to-account-id
                  nil)]
          ;; TODO: return a well formatted page
          tr)

        (lr/ring-response {:status 404
                           :body (pr-str "unknown action" action)}))
      ;; TODO:delete on success
      ))
  )
