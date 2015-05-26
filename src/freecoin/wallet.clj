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

   [hiccup.page :as page]
   [formative.core :as fc]
   [formative.parse :as fp]

   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]

   [ring.util.io :refer [piped-input-stream]]

   [freecoin.secretshare :as ssss]

   [freecoin.params :as param]
   [freecoin.random :as rand]
   [freecoin.utils :as util]
   [freecoin.auth :as auth]

   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]
   [freecoin.nxt :as nxt]

   [cheshire.core :as cheshire]

   [clj.qrgen :as qr]

;;   [taoensso.nippy :as nippy]

   [autoclave.core :as autoclave]
   )
  )


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



(defn find-wallet [request key value]
  {:pre [(keyword? key)]
   :post [(coll? %)]}
  "Find a wallet in the database using the id, which can be the name or
  email or NXT Reed-Solomon or numeric address."
  (storage/find-by-key 
               (get-in request [:config :db-connection])
               "wallets" {key value}))

(defresource card [request]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  :handle-ok      #(format-card-html (:wallet %))
  :handle-created #(format-card-json (:wallet %))
)

(defresource find-card [request key value]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  :exists?         {::found (first (find-wallet request (keyword key) value))}
  :handle-ok       #(format-card-html (::found %))
  :handle-created  #(format-card-json (::found %))
  )

(defresource qrcode [request name]
  :allowed-methods [:get]
  :available-media-types ["image/png"]
  :authorized? (fn [ctx] (auth/check request))
  :handle-ok #(if (nil? name)
                ;; name is the currently logged in user
                (let [wallet (:wallet %)]
                  (if (empty? wallet) ""
                      (qr/as-input-stream
                       (qr/from (format "http://%s:%d/give/%s"
                                        (:address param/host)
                                        (:port param/host)
                                        (:accountRS wallet))))
                      ))
                ;; else a name is specified
                (let [wallet (first (find-wallet request :name name))]
                  (if (empty? wallet) ""
                      (qr/as-input-stream
                       (qr/from (format "http://%s:%d/give/%s"
                                        (:address param/host)
                                        (:port param/host)
                                        (:accountRS wallet))))                      
                      ))
                ))


(defresource give [request recipient quantity]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  ;; TODO: if none, get NXT from faucet account
  ;; to cover the transaction fee. also check a maximum
  ;; limit of transactions per day.
  :handle-ok (fn [ctx] (auth/check request))
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
               (page/html5 (fc/render-form wallet-create-form))))

(defn parse-hybrid-form [form-spec request]
  (case (get-in request [:headers "content-type"])
    "application/x-www-form-urlencoded"
    (fp/with-fallback
      (fn [problems] {:status :error :problems problems})
      {:status :ok
       :data (fp/parse-params form-spec (:params request))})

    "application/json"
    {:status :ok
     :data (cheshire/parse-string (autoclave/json-sanitize (slurp (:body request))) true)}
    
    {:status :error :problems "unknown content type"}))

(defresource create [request]
;; Files a request to create a wallet, accepting a json structure
;; containing name and email. Checks if the name doesn't already
;; exists, if succesful returns a json structure containing the
;; 'confirmation' field which is the hash to be used to confirm
;; creation via GET url /wallet/create/confirmation-hash.

  :allowed-methods [:post]
  :available-media-types ["application/json"]

  :exists? (fn [ctx]
             (let [{status :status params :data problems :problems} (parse-hybrid-form wallet-create-form request)]
               (case status
                 :ok
                 (let [db (get-in request [:config :db-connection])
                       ;; TODO: optimize using redis k/v for this unauthenticated lookup
                       dup (storage/find-one db "wallets" {:name (:name params)})]
                   ;; TODO: truncate fields to length for security
                   (if (empty? dup)
                     ;; no duplicates found
                     {::status :ok ::params params}
                     ;; else
                     {::status :error ::params params ::problems "Duplicate username"}
                     ))
                   
                 :error
                 {::status :error ::params params ::problems problems})))

  :post! (fn [ctx]
           (case (::status ctx)
             :ok
             {::confirmation (ssss/hash-encode-num
                               param/encryption (:integer (rand/create 16 2.5)))}

             {}))

  ;; :post-redirect? (fn [ctx] (response/redirect  (format "/open/%s" (::id ctx))))
  ;;  :respond-with-entity? true
  :handle-created (fn [ctx]
                    (if (= :error (::status ctx))
                      {:error (::problems ctx)}
                      
                      ;; insert request
                      (let [name (get-in ctx [::params :name])
                            email (get-in ctx [::params :email])
                            confirm (::confirmation ctx)
                            stored (storage/insert 
                                    (get-in request [:config :db-connection])
                                    "confirms"
                                    {:_id   confirm
                                     :name  name
                                     :email email})]
                        (case (get-in request [:headers "content-type"])
                          "application/json"
                          {:body stored
                           :confirm (str "/wallet/create/" confirm)}

                          "application/x-www-form-urlencoded"
                          {:body stored
                           :confirm (str "<a href=\"/wallet/create/" confirm "\">confirm link</a>")})))))

(defresource confirm_create [request confirmation]
  :allowed-methods [:get]
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
  
  :handle-ok (fn [ctx]
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
                 {:debug (util/trace)
                  :error "Confirmation not found."
                  :id (::confirmation ctx)}))

  )

              
;; Reminder, but NEVER show nxtpass!
;; {:nxtpass (fxc/unlock-secret
;;            param/encryption
;;            (auth/get-secret request apikey) (:slice apikey))}
;;-----------------------------------------------------------
              
