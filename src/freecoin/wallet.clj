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
  (:require
   [clojure.string :as str]

   [formative.core :as fc]
   [formative.parse :as fp]

   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]

   [ring.util.io :refer [piped-input-stream]]

   [cheshire.core :as cheshire]

   [clj.qrgen :as qr]

;;   [taoensso.nippy :as nippy]

   [autoclave.core :as autoclave]

   [freecoin.secretshare :as ssss]

   [freecoin.params :as param]
   [freecoin.random :as rand]
   [freecoin.utils :as utils]
   [freecoin.auth :as auth]

   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]
   [freecoin.nxt :as nxt]

   [freecoin.views :as views]

   )
  )

(defn unauthorized-response [ctx] "Sorry, you are not signed in")

(defn- format-card-html [card]
  {:post [(contains? % :address)]}

  (if (empty? card) {:address "not found."}
      {:QR (format "<img src=\"/qrcode/%s\" alt=\"QR\">" (:name card))
       :name (:name card)
       :email (:email card)
       :address (:accountRS card)})
  )

(defn- format-card-json [card]
  {:post [(string? %)]}
  (if (empty? card) "{\"address\": \"not found.\"}"
      (cheshire/generate-string
       ;; TODO: make an inline image of the qrcode, see:
       ;; http://www.websiteoptimization.com/speed/tweak/inline-images/
       {:QR "<img src=\"/qrcode\" alt=\"QR\">"
        :name (:name card)
        :email (:email card)
        :address (:accountRS card)}
       ))
  )

(defrecord wallet
    [_id name email public-key private-key
     blockchains blockchain-accounts blockchain-secrets])

(defn new [name email]
  (wallet. (:string (rand/create 20)) ;; unique id
           name
           email
           nil ;; public key
           nil ;; private key
           [] ;; blockchains
           [] ;; blockchain-accounts
           [] ;; blockchain-secrets
           )
  )

(defresource card [request]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (:result (auth/check request))

  :handle-unauthorized   unauthorized-response
  :handle-ok      #(format-card-html (:wallet %))
  :handle-created #(format-card-json (:wallet %))
)

(def find-wallet-form-spec
  {:fields [{:name :field
             :type :select
             :options ["name" "email"]}
            {:name :value :type :text}]
   :validations [[:required [:field :value]]]
   :action "/wallets"
   :method "get"})

(defresource find-wallet-form [request]
  :allowed-methods       [:get]
  :available-media-types ["text/html"]
  :authorized?           (:result (auth/check request))
  ;; :handle-unauthorized   {:status 200
  ;;                         :body (:problem (auth/check request))}

  :handle-ok             (views/render-page views/simple-form-template
                                  {:title "Find wallet"
                                   :heading "Search for a wallet"
                                   :form-spec find-wallet-form-spec}))

(defn find-wallets [db query]
  (storage/find-by-key db "wallets" query))

(defn request->wallet-query [{:keys [params] :as request}]
  (if-let [{:keys [field value]} (utils/select-all-or-nothing params [:field :value])]
    {(keyword field) value}
    {}))

(defn render-wallet [wallet]
  [:li {:style "margin-top: 1em; width: 200px;"}
   [:div {:style "border: solid 1px; padding: 1em;"}
    [:span (str "name: " (:name wallet))]
    [:br]
    [:span (str " email: " (:email wallet))]
    [:br]
    [:img {:src (format "/qrcode/%s" (:name wallet))}]]])

(defn wallets-template [{:keys [wallets] :as content}]
  [:div
   (if (empty? wallets)
     [:span (str "No wallets found")]
     [:ul {:style "list-style-type: none;"}
      (for [wallet wallets]
        (render-wallet wallet))])])

(defresource wallets [request]
  :service-available? {::db (get-in request [:config :db-connection])}
  :allowed-methods       [:get]
  :available-media-types ["text/html"]
  :authorized?           (:result (auth/check request))

  :handle-unauthorized   unauthorized-response
  :handle-ok             (fn [ctx]
                           (let [wallets (->> request
                                              request->wallet-query
                                              (find-wallets (::db ctx)))]
                             (views/render-page wallets-template {:title "wallets"
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
                        (qr/from (format "http://%s:%d/give/%s"
                                         (:address param/host)
                                         (:port param/host)
                                         (:accountRS wallet))))
                       ))

                 ;; else a name is specified
                 (let [wallet (first (storage/find-by-key (get-in request [:config :db-connection])
                                                          "wallets" {:name id}))]
                   (if (empty? wallet) ""
                       (qr/as-input-stream
                        (qr/from (format "http://%s:%d/give/%s"
                                         (:address param/host)
                                         (:port param/host)
                                         (:accountRS wallet))))
                       ))
                 )))


(defresource give [request recipient quantity]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (:result (auth/check request))

  :handle-unauthorized   unauthorized-response
  ;; TODO: if none, get NXT from faucet account
  ;; to cover the transaction fee. also check a maximum
  ;; limit of transactions per day.
  :handle-ok (:wallet (auth/check request))
  )



;; Methods:
;; create
;; confirm_create (request-id)
;; open


(def wallet-create-form
  {:fields [{:name :name :type :text}
            {:name :email :type :email}]
   :validations [[:required [:name :email]]]})

(defresource create-form [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (views/render-page views/simple-form-template
                                  {:title "Create wallet"
                                   :heading "Create a new wallet"
                                   :form-spec wallet-create-form})))

(defn parse-hybrid-form [form-spec {:keys [request] :as ctx}]
  (case (::content-type ctx)
    "application/x-www-form-urlencoded"
    (fp/with-fallback
      (fn [problems] {:status :error :problems problems})
      {:status :ok
       :data (fp/parse-params form-spec (:params request))})

    "application/json"
    (let [data (cheshire/parse-string (autoclave/json-sanitize (slurp (:body request))) true)]
      (fp/with-fallback
        (fn [problems] {:status :error :problems problems})
        {:status :ok :data (fp/parse-params form-spec data)}))

    {:status :error :problems [{:keys [] :msg (str "unknown content type: " (::content-type ctx))}]}))

(def response-representation
  {"application/json" "application/json"
   "application/x-www-form-urlencoded" "text/html"})

(defresource create [request]
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
              (let [{:keys [status data problems]} (parse-hybrid-form
                                                    wallet-create-form
                                                    ctx)]
                (case status
                  :ok
                  ;; TODO: optimize using redis k/v for this unauthenticated lookup
                  (if (storage/find-one (::db ctx) "wallets" {:name (:name data)})

                    [false {::user-data data
                            ::problems [{:keys ["name"] :msg "username already exists"}]
                            :representation {:media-type (get response-representation
                                                              (::content-type ctx))}}]

                    [true {::user-data data}])

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
                        {:location (str "/wallet/create/" (:_id confirmation))})

                      ;; TODO: handle default case
                      ))

  :handle-created (fn [ctx]
                    (let [confirmation (::confirmation ctx)]
                      (case (::content-type ctx)
                        "application/json"
                        {:body confirmation
                         :confirm (str "/wallet/create/" (:_id confirmation))}

                        ;; TODO: handle default case
                        ))))

(def wallet-confirm-create-form {:submit-label "Confirm"})

(defresource confirm-create-form [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx] (views/render-page views/simple-form-template
                                          {:title "Confirm wallet creation"
                                           :heading "Please confirm the creation of your wallet"
                                           :form-spec wallet-confirm-create-form})))

(defresource confirm-create [request confirmation]
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
                            secret (fxc/create-secret param/encryption)
                            cookie-data (str/join "::" [(:cookie secret) (:_id secret)])
                            secret-without-cookie (dissoc secret :cookie)
                            nxt-data (nxt/getAccountId
                                      {:secret (fxc/unlock-secret
                                                param/encryption
                                                secret-without-cookie
                                                (:cookie secret))})]
                        ;; delete the confirmation entry from the db
                        (storage/remove-by-id db "confirms" (:_id params))

                        ;; insert in the secrets database
                        (storage/insert db "secrets" secret-without-cookie)

                        ;; insert in the wallet database
                        (storage/insert db "wallets"
                                        (conj nxt-data {:name  (:name params)
                                                        :email (:email params)
                                                        :_id (:_id secret-without-cookie)}))

                        ;; return the apikey cookie
                        (ring-response {:headers {"Location" (ctx :location)}
                                        :session {:cookie-data cookie-data}
                                        :apikey cookie-data})
                        ;; TODO: give PINs
                        ;; send backup (show QR, also per email?)

                        )
                      ;; else confirmation not found
                      {:debug (utils/trace)
                       :error "Confirmation not found."
                       :id (::confirmation ctx)})))


;; Reminder, but NEVER show nxtpass!
;; {:nxtpass (fxc/unlock-secret
;;            param/encryption
;;            (auth/get-secret request apikey) (:slice apikey))}
;;-----------------------------------------------------------
