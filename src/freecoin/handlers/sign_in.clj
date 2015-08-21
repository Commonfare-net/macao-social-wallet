(ns freecoin.handlers.sign-in
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [ring.util.response :as r]
            [stonecutter-oauth.client :as soc]
            [stonecutter-oauth.jwt :as sjwt]
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

(lc/defresource sso-callback [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :exists? (fn [ctx]
             (let [code (get-in ctx [:request :params :code])]
               (when-let [token-response (soc/request-access-token! sso-config code)]
                 {::user-id (get-in token-response [:user-info :user-id])
                  ::email (get-in token-response [:user-info :email])
                  ::email-verified (get-in token-response [:user-info :email_verified])})))
  :handle-ok (fn [ctx]
               (lr/ring-response (assoc-in (r/redirect "/landing-page")
                                           [:session :user-id] (::user-id ctx)))))
