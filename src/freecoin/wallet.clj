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

(ns freecoin.wallet
  ;; (:import [freecoin.blockchain stub])
  (:require
   [clojure.string :as str]

   [formative.core :as fc]
   [formative.parse :as fp]

   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]

   [ring.util.io :refer [piped-input-stream]]

   [clj.qrgen :as qr]

   [clavatar.core]

;;   [taoensso.nippy :as nippy]


   [freecoin.secretshare :as ssss]

   [freecoin.params :as param]
   [freecoin.random :as rand]
   [freecoin.utils :as utils]
   [freecoin.auth :as auth]

   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]
   [freecoin.blockchain :as blockchain]

   [freecoin.views :as views]

   )
  )

;; A wallet can contain multiple, non duplicate blockchain accounts and is unique to a participant.
(defrecord wallet
    [_id                 ;; unique id
     name                ;; identifier, case insensitive, space counts
     email               ;; working email account (can be restricted to a list of accepted domains)
     public-key          ;; public asymmetric key for off-the-blockchain message encryption
     private-key         ;; private asymmetric key for off-the-blockchain message encryption
     blockchains         ;; list of blockchains and public account ids
     blockchain-secrets  ;; list of secrets enabling the access to private blockchain operations
     ])

(def participants-form-spec
  {:fields [{:name :field
             :type :select
             :options ["name" "email"]}
            {:name :value :type :text}]
   :validations [[:required [:field :value]]]
   :action "/participants/find"
   :method "get"})

(defresource participants-form [request]
  :allowed-methods       [:get]
  :available-media-types ["text/html"]
  :authorized?           (:result (auth/check request))
  ;; uncomment this to deny access to non-participants
  ;; :unauthorized          (:problem (auth/check request))

  ;; :handle-unauthorized   {:status 200
  ;;                         :body (:problem (auth/check request))}

  :handle-ok             (views/render-page views/simple-form-template
                                  {:title "Find wallet"
                                   :heading "Search for a wallet"
                                   :form-spec participants-form-spec})
  )

(defn render-wallet [wallet]
  [:li {:style "margin: 1em"}
   [:div {:class "card pull-left" }
    [:span (str "name: " (:name wallet))]
    [:br]
    [:span (str "email: " (:email wallet))]
    [:br]
    [:span {:class "qrcode pull-left"}
     [:img {:src (format "/qrcode/%s" (:name wallet))} ]]
    [:span {:class "gravatar pull-right"}
     [:img {:src (clavatar.core/gravatar (:email wallet) :size 87 :default :mm)}]]
    ]])

(defn welcome-template [{:keys [wallet] :as content}]
   (if (empty? wallet)
     [:span (str "Error creating wallet")]
     (let [name (:name wallet)
           email (:email wallet)]
       [:h1 (str "Welcome " name)]
       [:p (str "We have sent an email to the address " email "with recommendations on how to store your wallet safely")]
       [:p "You can access " [:a {:href "/" } "your balance here." ]]
       [:ul {:style "list-style-type: none;"}
        (render-wallet wallet)])
     )
   )

(defn balance-template [{:keys [wallet balance] :as content}]
   (if (empty? wallet)
     [:span (str "No wallet found")]
     [:div
      [:ul {:style "list-style-type: none;"}
      (render-wallet wallet)]
      [:div {:class "balance pull-left"}
       (str "Balance: " balance)]]
     )
   )

(defn participants-template [{:keys [wallets] :as content}]
  [:div
   (if (empty? wallets)
     [:span (str "No participant found")]
     [:ul {:style "list-style-type: none;"}
      (for [wallet wallets]
        (render-wallet wallet))])])


(defn request->wallet-query [{:keys [params] :as request}]
  (if-let [{:keys [field value]} (utils/select-all-or-nothing params [:field :value])]
    {(keyword field) value}
    {}))


(defresource participants-find [request]
  :service-available?    {::db (get-in request [:config :db-connection])}
  :allowed-methods       [:get]
  :available-media-types ["text/html"]
  :authorized?           (:result (auth/check request))
  :handle-unauthorized   (:problem (auth/check request))

  :handle-ok             (fn [ctx]
                           (let [wallets (->> request
                                              request->wallet-query
                                              (storage/find-by-key (::db ctx) "wallets"))]
                             (views/render-page participants-template {:title "wallets"
                                                                       :wallets wallets}))))
  
(defresource qrcode [request id]
  :allowed-methods [:get]
  :available-media-types ["image/png"]
  :authorized?           (:result (auth/check request))
  :handle-unauthorized   (:problem (auth/check request))

  :handle-ok (fn [ctx]
               (if (nil? id) ;; name is the currently logged in user
                 (let [wallet (auth/get-wallet request)]
                   (if (empty? wallet) ""
                       (qr/as-input-stream
                        (qr/from (format "http://%s:%d/send/to/%s"
                                         (:address param/host)
                                         (:port param/host)
                                         (:name wallet))))
                       ))

                 ;; else a name is specified
                 (let [wallet (first (storage/find-by-key
                                      (get-in request [:config :db-connection])
                                      "wallets" {:name (ring.util.codec/percent-decode id)}))]

                   (if (empty? wallet) ""
                       (qr/as-input-stream
                        (qr/from (format "http://%s:%d/send/to/%s"
                                         (:address param/host)
                                         (:port param/host)
                                         (:name wallet))))
                       ))
                 )))

;; Methods:
;; create
;; confirm_create (request-id)
;; open


(def wallet-create-form
  {:fields [{:name :name :type :text}
            {:name :email :type :email}]
   :validations [[:required [:name :email]]]})

(defresource get-create [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (if (:result (auth/check request))
                 ;; user has already a wallet? then show balance
                 (views/render-page
                  balance-template {:title "Balance"
                                    :wallet (:wallet (auth/check request))}
                  )
                 ;; else propose to create a wallet
                 (views/render-page views/simple-form-template
                                    {:title "Create wallet"
                                     :heading "Create a new wallet"
                                     :form-spec wallet-create-form}))
               ))




(defresource post-create [request]
  ;; Files a request to create a wallet, accepting a json structure
  ;; containing name and email. Checks if the name doesn't already
  ;; exists, if succesful returns a json structure containing the
  ;; 'confirmation' field which is the hash to be used to confirm
  ;; creation via GET url /wallet/create/confirmation-hash.


  ;; Liberator doesn't provide a way to initialise the context;
  ;; service-available? is the first decision point, and defaults to
  ;; 'true'.   Thus, we'll use it to add some convenient values to our context.
  ;; Will create a PR for liberator to add an 'initialise' key. DM
  :service-available? {::db (get-in request [:config :db-connection])
                       ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:post]
  :available-media-types ["application/json"]

  :allowed? (fn [ctx]
              (let [{:keys [status data problems]}
                    (views/parse-hybrid-form request
                     wallet-create-form
                     (::content-type ctx))]
                (case status
                  :ok
                  ;; TODO: optimize using redis k/v for this unauthenticated lookup
                  (if (storage/find-one (::db ctx) "wallets" {:name (:name data)})
                    
                    ;; found a duplicate
                    [false {::user-data data
                            ::problems [{:keys ["name"] :msg "username already exists"}]
                            :representation {:media-type (get views/response-representation
                                                              (::content-type ctx))}}]

                    [true {::user-data data}])

                  :error
                  [false {::user-data data
                          ::problems problems
                          :representation {:media-type
                                           (get views/response-representation (::content-type ctx))}}]

                  ;; TODO: handle default case
                  )))

  :handle-forbidden (fn [ctx]
                      (case (::content-type ctx)
                        "application/json" {:reason (::problems ctx)}

                        "application/x-www-form-urlencoded"
                        (views/render-page views/simple-form-template
                                           {:title "Create wallet"
                                            :heading "Create a new wallet"
                                            :form-spec (assoc wallet-create-form
                                                              :problems (::problems ctx)
                                                              :values (::user-data ctx))})))

  :post! (fn [ctx]
           (let [name (get-in ctx [::user-data :name])
                 email (get-in ctx [::user-data :email])
                 confirmation-code (ssss/hash-encode-num
                                    param/encryption (:integer (rand/create 16)))
                 stored-confirmation (storage/insert
                                      (::db ctx)
                                      "confirms"
                                      {:_id   confirmation-code
                                       :name  name
                                       :email email})]
             {::confirmation stored-confirmation}))

  :post-redirect? (fn [ctx]
                    (case (::content-type ctx)
                      "application/json" false

                      "application/x-www-form-urlencoded"
                      (let [confirmation (::confirmation ctx)]
                        {:location (str "/wallets/" (:_id confirmation))})

                      ;; TODO: handle default case
                      ))

  :handle-created (fn [ctx]
                    (let [confirmation (::confirmation ctx)]
                      (case (::content-type ctx)
                        "application/json"
                        {:body confirmation
                         :confirm (str "/wallets/" (:_id confirmation))}

                        ;; TODO: handle default case
                        ))))

(defresource get-create-confirm [request confirmation]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (views/render-page
                views/simple-form-template
                {:title "Confirm wallet creation"
                 :heading "Please confirm the creation of your wallet"
                 :form-spec {:submit-label "Confirm"}})))

(defresource post-create-confirm [request confirmation]
  :service-available? {::db (get-in request [:config :db-connection])
                       ::content-type (get-in request [:headers "content-type"])}

  :allowed-methods [:post]
  :available-media-types ["text/html"]
  :exists? (fn [ctx]
             (let [db (get-in request [:config :db-connection])
                   cc (storage/find-by-id db "confirms" confirmation)]
               (if (empty? cc)
                 ;; no confirmation found
                 {::found false ::confirmation confirmation}
                 ;; entry found, proceed
                 {::found true ::params cc}
                 )))

  :handle-created (fn [ctx]
                    ;; the confirmation was found
                    (if (::found ctx)
                      ;; proceed with the wallet creation
                      (let [params (::params ctx)
                            db (get-in request [:config :db-connection])
                            new-wallet (blockchain/create-account
                                        (blockchain/new-stub db) ;; default blockchain
                                        (wallet. ""
                                                 (:name params) (:email params)
                                                 nil ;; public key
                                                 nil ;; private key
                                                 {} ;; blockchains
                                                 {} ;; blockchain-secrets
                                                 ))
                            secret (get-in new-wallet [:blockchain-secrets :STUB])
                            secret-without-cookie (dissoc secret :cookie)
                            cookie-data (str/join "::" [(:cookie secret) (:_id secret)])]

                        (if (contains? new-wallet :problem)
                          (utils/log! (::error new-wallet))
                          (let []
                            ;; else


                            ;; delete the confirmation entry from the db
                            (storage/remove-by-id db "confirms" (:_id params))

                            ;; ;; insert in the secrets database
                            ;; (storage/insert db "secrets" secret-without-cookie)

                            ;; insert in the wallet database, use the shamir's generated UID as _id
                            (storage/insert db "wallets" (assoc new-wallet :_id (:_id secret) ))

                            ;; return the apikey cookie
                            (ring-response {:headers {"Location" (ctx :location)}
                                            :session {:cookie-data cookie-data}
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


                      ;; else confirmation not found
                      {:debug (utils/trace)
                       :error "Confirmation not found."
                       :id (::confirmation ctx)}
                      )
                    )
  
  )

                            ;; (utils/log! ::ACK :cookie cookie-data)
                            ;; (utils/log! ::ACK :secret secret)
                            ;; (utils/log! ::ACK :wallet (pr-str new-wallet))

                            ;; nxt-data (nxt/getAccountId
                            ;;           {:secret (fxc/unlock-secret
                            ;;                     param/encryption
                            ;;                     secret-without-cookie
                            ;;                     (:cookie secret))})]

(defresource balance-show [request]
  :service-available?    {::db (get-in request [:config :db-connection])}
  :allowed-methods       [:get]
  :available-media-types ["text/html"]
  :authorized?           (:result (auth/check request))
  :handle-unauthorized   (:problem (auth/check request))

  :handle-ok (fn [ctx]
               (let [wallet (auth/get-wallet request)]
               ;; (utils/log! 'ACK 'balance-show (clojure.pprint/pprint wallet))
                 (views/render-page
                  balance-template {:title "Balance"
                                    :wallet wallet
                                    :balance (blockchain/get-balance 
                                              (blockchain/new-stub (::db ctx))
                                              wallet)}
                  )))
  )



;; Reminder, but NEVER show nxtpass!
;; {:nxtpass (fxc/unlock-secret
;;            param/encryption
;;            (auth/get-secret request apikey) (:slice apikey))}
;;-----------------------------------------------------------
