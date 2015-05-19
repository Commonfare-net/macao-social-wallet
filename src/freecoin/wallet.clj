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

   )
  )

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
             (let [params (slurp (get-in ctx [:request :body]))
                   mapped_params (cheshire/parse-string params true)
                   param_name (:name mapped_params)
                   param_email (:email mapped_params)
                   db (get-in request [:config :db-connection])
                   ;; TODO: optimize using redis k/v for this unauthenticated lookup
                   dup (storage/find-one db "wallets" {:name param_name})]

               ;; TODO: truncate fields to length for security
               (if (empty? dup)
                 ;; no duplicates found
                 {::params mapped_params
                  ::duplicate false}
                 ;; else
                 {::params mapped_params
                  ::duplicate true}
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
                      ;; return duplicate error
                      {;; :debug (util/trace)
                       :body (::params ctx)
                       :id {:error "duplicate"}}
                      
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
                           {::found false
                            ::confirmation confirmation}
                           ;; entry found, proceed
                           {::found true
                            ::params cc}
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
                   (storage/insert db "wallets" (conj nxt-data {:name  (get-in ctx [::params :name])
                                                                :email (get-in ctx [::params :email])
                                                                :_id (:_id secret-without-cookie)}))

                   ;; return the apikey cookie
                   (ring-response {:headers {"Location" (ctx :location)}
                                   :session {:cookie-data cookie-data}
                                   :apikey cookie-data}
                                  )

                   )
                 ;; else confirmation not found
                 {:debug (util/trace)
                  :error "Confirmation not found."
                  :id (::confirmation ctx)}))

  )

(defresource balance [request]
  :allowed-methods [:get :post]
  :available-media-types ["text/html" "application/json"]
  :authorized? (auth/check request)


  :handle-ok (fn [ctx]
               (let [apikey (:apikey ctx)
                     wallet (:wallet ctx)]
                 (if (and wallet apikey)
                   (clojure.set/rename-keys
                    (dissoc
                     (conj wallet {:QR "<img src=\"/wallet/qrcode\" alt=\"QR\">"}
                           (nxt/getBalance (:account wallet))

                           ;; TODO: remove this when testing is over (NEVER show nxtpass)
                           ;; {:nxtpass (fxc/unlock-secret
                           ;;            param/encryption
                           ;;            (auth/get-secret request apikey) (:slice apikey))}
                           ;;-----------------------------------------------------------

                           )
                     :_id :account :requestProcessingTime :publicKey)
                    {:accountRS :Konto})

                   {:Wallet "not found."})
               ))
  )


(defresource qrcode [request]
  :allowed-methods [:get]
  :available-media-types ["image/png"]
  :authorized? (fn [ctx] (auth/check request))
  :handle-ok (fn [ctx]
               (let [apikey (:apikey ctx)
                     wallet (:wallet ctx)]
                 (if (empty? wallet) ""
                     (qr/as-input-stream (qr/from (:accountRS wallet))))
               ))
  )

(defresource give [request recipient quantity]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :authorized? (fn [ctx] (auth/check request))

  :handle-ok (fn [ctx] (auth/check request))
  )
