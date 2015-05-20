(ns freecoin.wallet
  (:require
   [clojure.string :as str]

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

   [cheshire.core :refer :all :as cheshire]

   [clj.qrgen :as qr]

   [taoensso.nippy :as nippy]

   [autoclave.core :refer :all]
   )
  )


(defn find-wallet [request key value]
  {:pre [(keyword? key)]
   :post [(coll? %)]}
  "Find a wallet in the database using the id, which can be the name or
  email or NXT Reed-Solomon or numeric address."
  (storage/find-by-key 
               (get-in request [:config :db-connection])
               "wallets" {key value}))


;; Methods:
;; create
;; confirm_create (request-id)
;; open

(defresource create [request]

;; Files a request to create a wallet, accepting a json structure
;; containing name and email. Checks if the name doesn't already
;; exists, if succesful returns a json structure containing the
;; 'confirmation' field which is the hash to be used to confirm
;; creation via GET url /wallet/create/confirmation-hash.

  :allowed-methods [:post]
  :available-media-types ["application/json"]                  

  :exists? (fn [ctx]
             (let [params (json-sanitize (slurp  (get-in ctx [:request :body])))
                   mapped_params  (cheshire/parse-string params true)
                   param_name (:name mapped_params)
                   param_email (:email mapped_params)
                   db (get-in request [:config :db-connection])
                   ;; TODO: optimize using redis k/v for this unauthenticated lookup
                   dup (storage/find-one db "wallets" {:name param_name})]

               ;; TODO: truncate fields to length for security
               (if (empty? dup)
                 ;; no duplicates found
                 {::params mapped_params ::duplicate false}
                 ;; else
                 {::params mapped_params ::duplicate true}
                 )
               ))



  :post! (fn [ctx]
           (if (::duplicate ctx) nil
               {::confirmation (ssss/hash-encode-num
                                param/encryption (:integer (rand/create 16 2.5)))}
               ))

  ;; :post-redirect? (fn [ctx] (response/redirect  (format "/open/%s" (::id ctx))))
  ;;  :respond-with-entity? true
  :handle-created (fn [ctx]
                    (if (::duplicate ctx)
                      {:error 'DUP :explanation "Name already exists, choose another."}
                      
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
                        
                        {:body stored
                         :confirm (str "/wallet/create/" confirm)}
                        ))
                    )
  )


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


(defn- format-card-html [ctx]
  {:post [(contains? % :address)]}

  (if (empty? ctx) {:address "not found."}
      {:QR "<img src=\"/wallet/qrcode\" alt=\"QR\">"
       :name (:name ctx)
       :email (:email ctx)
       :address (:accountRS ctx)})
  )

(defn- format-card-json [ctx]
  {:post [(string? %)]}
  (if (empty? ctx) "{\"address\": \"not found.\"}"
      (cheshire/generate-string
       ;; TODO: make an inline image of the qrcode, see:
       ;; http://www.websiteoptimization.com/speed/tweak/inline-images/
       {:QR "<img src=\"/wallet/qrcode\" alt=\"QR\">"
        :name (:name ctx)
        :email (:email ctx)
        :address (:accountRS ctx)}
       ))
  )
              
;; Reminder, but NEVER show nxtpass!
;; {:nxtpass (fxc/unlock-secret
;;            param/encryption
;;            (auth/get-secret request apikey) (:slice apikey))}
;;-----------------------------------------------------------
              

(defresource balance [request]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  :handle-ok      #(format-card-html (:wallet %))
  :handle-created #(format-card-json (:wallet %))
)

(defresource qrcode [request]
  :allowed-methods [:get]
  :available-media-types ["image/png"]
  :authorized? (fn [ctx] (auth/check request))
  :handle-ok (fn [ctx]
               (let [wallet (:wallet ctx)]
                 (if (empty? wallet) ""
                     (qr/as-input-stream
                      (qr/from (format "http://localhost:8000/give/%s" (:accountRS wallet))))
               ))
  ))


(defresource find [request key value]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  :exists?         {::found (first (find-wallet request (keyword key) value))}
  :handle-ok       #(format-card-html (::found %))
  :handle-created  #(format-card-json (::found %))
  )

(defresource give [request recipient quantity]
  :allowed-methods       [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized?           (auth/check request)

  ;; TODO: if none, get NXT from faucet account
  ;; to cover the transaction fee. also check a maximum
  ;; limit of transactions per day.
  :handle-ok (fn [ctx] (auth/check request))
  )
