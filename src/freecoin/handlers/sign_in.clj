(ns freecoin.handlers.sign-in
  (:require [liberator.core :as lc]
            [stonecutter-oauth.client :as sc]
            [stonecutter-oauth.jwt :as sjwt]
            [freecoin.views :as fv]
            [freecoin.views.landing-page :as landing-page]))

(lc/defresource landing-page [sso-config]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  :handle-ok (-> {:sso-sign-in-link-url (:auth-provider-url sso-config)}
                 landing-page/landing-page
                 fv/render-page))
