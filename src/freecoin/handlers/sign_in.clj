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
  :allowed? (fn [ctx]
              (when-let [code (get-in ctx [:request :params :code])]
                (try
                  (when-let [token-response (soc/request-access-token! sso-config code)]
                    {::token-response token-response})
                  (catch Exception e nil))))
  :handle-forbidden (lr/ring-response (r/redirect "/landing-page"))
  :exists? (fn [ctx]
             (let [token-response (::token-response ctx)
                   user-id (get-in token-response [:user-info :user-id])
                   email (get-in token-response [:user-info :email])
                   email-verified (get-in token-response [:user-info :email_verified])
                   name (first (s/split email #"@"))]
               (when-let [wallet-cookie (create-wallet db-connection name email)]
                 {::user-id user-id
                  ::email email
                  ::email-verified email-verified
                  ::cookie-data wallet-cookie})))
  :handle-ok (fn [ctx]
               (lr/ring-response
                (-> (r/redirect "/landing-page")
                    (assoc-in [:session :cookie-data] (::cookie-data ctx))
                    (assoc-in [:session :user-id] (::user-id ctx)))))
  :handle-not-found (lr/ring-response (r/redirect "/landing-page")))
