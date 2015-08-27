(ns freecoin.handlers.sign-in
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [clojure.string :as s]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as sjwt]
            [freecoin.storage :as storage]
            [freecoin.blockchain :as blockchain]
            [freecoin.utils :as utils]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]))

(lc/defresource landing-page
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> {:sign-in-url "/sign-in-with-sso"}
                 landing-page/landing-page
                 fv/render-page))

(lc/defresource sign-in [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (fn [ctx]
               (lr/ring-response (soc/authorisation-redirect-response sso-config))))

(defn empty-wallet [name email]
  {:_id ""
   :name  name
   :email email
   :public-key nil
   :private-key nil
   :blockchains {}
   :blockchain-secrets {}})

(defn create-wallet [db-connection name email]
  (let [new-account
        (blockchain/create-account
         (blockchain/new-stub db-connection)
         (empty-wallet name email))
        secret (get-in new-account [:blockchain-secrets :STUB])
        secret-without-cookie (dissoc secret :cookie)
        cookie-data (s/join "::" [(:cookie secret) (:_id secret)])]
    (utils/log! ::ACK 'signin cookie-data)
    
    (if (contains? new-account :problem)
      (do
        (utils/log! (::error new-account)))
      (do
        (storage/insert db-connection "wallets"
                        (assoc new-account :_id (:_id secret)))
        cookie-data))))

(lc/defresource sso-callback [db-connection sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? (fn [ctx]
             (when-let [code (get-in ctx [:request :params :code])]
               (when-let [token-response (soc/request-access-token! sso-config code)]
                 (let [user-id (get-in token-response [:user-info :user-id])
                       email (get-in token-response [:user-info :email])
                       email-verified (get-in token-response [:user-info :email_verified])
                       name (first (s/split email #"@"))]
                   (when-let [wallet-cookie (create-wallet db-connection name email)]
                     {::user-id user-id
                      ::email email
                      ::email-verified email-verified
                      ::cookie-data wallet-cookie})))))
  :handle-ok (fn [ctx]
               (lr/ring-response
                (-> (r/redirect "/landing-page")
                    (assoc-in [:session :cookie-data] (::cookie-data ctx))
                    (assoc-in [:session :user-id] (::user-id ctx)))))
  :handle-not-found (lr/ring-response (r/redirect "/landing-page")))
