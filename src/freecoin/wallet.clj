(ns freecoin.wallet
  (:require
   [clojure.string :as str]

   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]

   [freecoin.secretshare :as ssss]

   [freecoin.params :as param]
   [freecoin.random :as rand]
   [freecoin.utils :as util]
   [freecoin.validation :as valid]
   [freecoin.response :as response]
   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]

   [cheshire.core :refer :all :as cheshire]

   )
  )


(defresource create [request]
  :allowed-methods [:post]
  :available-media-types ["application/json"]                  

  :exists? (fn [ctx]
             (let [params (slurp (get-in ctx [:request :body]))
                   mapped_params (cheshire/parse-string params true)
                   param_name (:name mapped_params)
                   param_email (:email mapped_params)
                   db (get-in request [:config :db-connection])
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
                      {:header (util/trace)
                       :body (::params ctx)
                       :id {:error "duplicate"}}
                      
                      ;; insert request
                      (let [name (get-in ctx [::params :name])
                            email (get-in ctx [::params :email])
                            stored (storage/insert 
                                    (get-in request [:config :db-connection])
                                    "confirms"
                                    {:_id   (::confirmation ctx)
                                     :name  name
                                     :email email})]
                        
                        {:header (util/trace)
                         :body stored
                         :id (::confirmation ctx)})
                      )
                    ))


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
                       secret-without-cookie (dissoc secret :cookie)]

                   ;; delete the confirmation entry from the db
                   (storage/remove-by-id db "confirms" (:_id params))

                   ;; insert the wallet in the database
                   (storage/insert
                    (get-in request [:config :db-connection])
                    "wallets" (conj secret-without-cookie {:name  (get-in ctx [::params :name])
                                                           :email (get-in ctx [::params :email])}))

                   ;; return a cookie
                   (ring-response {:headers {"Location" (ctx :location)}
                                   :session {:cookie-data cookie-data}
                                   :apikey cookie-data}
                                  )

                   )
                 ;; else confirmation not found
                 {:debug (util/trace)
                  :error "Confirmation not found."
                  :id (::confirmation ctx)}))
                 ;; (pages/template {:header (util/trace)
                 ;;                  :body (str "Confirmation not found: " (::confirmation ctx))
                 ;;                  :id (::confirmation ctx)})))
  )

(defresource open [request]
  :allowed-methods [:get :post]
  :available-media-types ["text/html" "application/json"]
  :exists? (fn [ctx]
             (let [cookie (get-in request [:session :cookie-data])
                   db (get-in request [:config :db-connection])]
               ;; safeguard
               (if (empty? cookie) {::wallet false}
                   
                   (let [slice (first (str/split cookie #"::"))
                         id (second (str/split cookie #"::"))
                         wallet (storage/find-by-id db "wallets" id)]
                     (if (empty? wallet) {::wallet false}
                         {::wallet wallet
                          ::slice slice})
                     ))))

  :handle-ok (fn [ctx]
               (let [wallet (::wallet ctx)]
                 (if wallet
                   (dissoc (conj wallet {:nxtpass (fxc/unlock-secret param/encryption
                                          (::wallet ctx) (::slice ctx))})
                           :slices :config)
                   {:error "Cannot open wallet."}))
               )
  )
