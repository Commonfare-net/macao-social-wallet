;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>
;; Andrei Biasprozvanny <abiaspro@thoughtworks.com>

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

(ns freecoin.transactions
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [simple-time.core :as time]
            [freecoin.blockchain :as blockchain]
            [freecoin.confirmations :as confirms]
            [freecoin.storage :as storage]
            [freecoin.views :as views]
            [freecoin.utils :as utils]
            [freecoin.context-helpers :as ch]
            [freecoin.auth :as auth]))

(defn transaction-form-spec [recipient]
  {:fields [{:name :amount :datatype :float}
            (if recipient
              {:name :recipient :type :hidden :value recipient}
              {:name :recipient :type :text})]
   :validations [[:required [:amount :recipient]]
                 [:min-val 0.01 [:amount]]]
   :action "/send"
   :method "post"})


(lc/defresource get-transaction-form [request recipient]
  :allowed-methods       [:get]
  :available-media-types ["text/html"]

  :authorized?           (:result (auth/check request))
  :unauthorized          (:problem (auth/check request))

  :handle-ok             (fn [ctx]
                           (views/render-template
                            views/simple-form-template
                            {:title "Make a transaction"
                             :heading (str "Send an amount to: " recipient)
                             :form-spec (transaction-form-spec recipient)}))
  )

(def response-representation
  {"application/json" "application/json"
   "application/x-www-form-urlencoded" "text/html"})

(lc/defresource post-transaction-form [wallet-store]
  :service-available? (fn [ctx] {::db (get-in ctx [:request :config :db-connection])
                                 ::content-type (get-in ctx [:request :headers "content-type"])})

  :allowed-methods       [:post]
  :available-media-types ["application/json"]

  :authorized?           (fn [ctx] (:result  (auth/check wallet-store (ch/context->signed-in-email ctx))))
  :unauthorized          (fn [ctx] (:problem (auth/check wallet-store (ch/context->signed-in-email ctx))))

  :allowed? (fn [ctx]
              (let [{:keys [status data problems]}
                    (views/parse-hybrid-form (:request ctx)
                                             (transaction-form-spec nil)
                                             (::content-type ctx))]
                (case status
                  :ok
                  (if (storage/find-one (:db (::db ctx)) "wallets" {:name (:recipient data)})
                    [true {::user-data data}]
                    [false {::user-data data
                            ::problems [{:keys ["recipient"] :msg "there is no recipient with that name"}]
                            :representation {:media-type (get views/response-representation
                                                              (::content-type ctx))}}])

                  :error
                  [false {::user-data data
                          ::problems problems
                          :representation {:media-type
                                           (get response-representation (::content-type ctx))}}]

                  ;; TODO: handle default case
                  )))

  :handle-forbidden (fn [ctx]
                      (case (::content-type ctx)
                        "application/json" {:reason (::problems ctx)}
                        "application/x-www-form-urlencoded"
                        (views/render-template
                         views/simple-form-template
                         {:title "Make a transaction"
                          :heading (str "Send freecoins")
                          :form-spec (assoc (transaction-form-spec nil)
                                            :problems (::problems ctx)
                                            :values (::user-data ctx))})
                        ;; see also in wallet how this can be handled
                        ))

  :post! (fn [ctx]
           (let [amount (get-in ctx [::user-data :amount])
                 recipient (get-in ctx [::user-data :recipient])
                 confirmation (confirms/create (::db ctx) "transaction"
                                               {:amount amount
                                                :recipient recipient})]
             {::confirmation confirmation}))

  :post-redirect? (fn [ctx]
                    (case (::content-type ctx)
                      "application/json" false
                      "application/x-www-form-urlencoded"
                      (::confirmation ctx)
                      ;; default
                      false))

  :handle-created (fn [ctx]
                    (let [confirmation (::confirmation ctx)]
                      (case (::content-type ctx)
                        "application/json"
                        (lr/ring-response {:headers {"Location" (:location confirmation)}})

                        ;; TODO: handle default case
                        ))))

(lc/defresource get-transaction-confirm [request confirmation]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (views/render-template
                views/simple-form-template
                {:title "Confirm transaction"
                 :heading "Please confirm to execute the transacton"
                 :form-spec {:submit-label "Confirm"}})))

(lc/defresource post-transaction-confirm [request confirmation]
  :service-available? {::db (get-in request [:config :db-connection])
                       ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:post]
  :available-media-types ["text/html"]

  :authorized?           (:result  (auth/check request))
  :handle-unauthorized   (:problem (auth/check request))

  :exists? (fn [ctx]
             (let [cc (storage/find-by-id (:db (::db ctx)) "confirms" confirmation)]
               (if (empty? cc)
                 ;; no confirmation found
                 {::found false ::confirmation confirmation}
                 ;; entry found, proceed
                 {::found true ::params cc}
                 )))

  :handle-created (fn [ctx]
                    (let [params (::params ctx)
                          wallet (auth/get-wallet request)
                          db     (::db ctx)]

                      (if (empty? wallet) {:status 401
                                           :body "Wallet not found."}

                            ;; else
                          (utils/pretty
                           (blockchain/make-transaction
                            (blockchain/new-stub (:db db)) wallet
                            (:amount params) (:recipient params)
                            nil) ;; secret is not used in STUB
                           )))))

(defn render-transaction [tx]
  (let [date (time/parse (:timestamp tx))]
    [:li {:style "margin: 1em"}
       [:span (time/format date :medium-date-time)] [:br]
       [:span (str (:amount tx) " :: " (:from tx) " -> " (:to tx))]]
    )
  )

(defn list-transactions-template [{:keys [transactions] :as content}]
  [:div
   (if (empty? transactions)
     [:span (str "No transaction found")]
      [:ul {:style "list-style-type: none;"}
       (for [tx transactions]
         (render-transaction tx))]
     )])

(lc/defresource get-all-transactions [request]
  :service-available? {::db (get-in request [:config :db-connection])
                       ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized?           (:result  (auth/check request))
  :handle-unauthorized   (:problem (auth/check request))

  :handle-ok (fn [ctx]
               (let [transactions (blockchain/list-transactions
                                   (blockchain/new-stub (:db (::db ctx))) (auth/get-wallet request))]
                 (views/render-template
                  list-transactions-template {:title "Transactions"
                                              :transactions transactions})))
  )
