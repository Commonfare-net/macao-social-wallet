(ns freecoin.handlers.sign-in
  (:require [liberator.core :as lc]
            [liberator.representation :as lr]
            [stonecutter-oauth.client :as sc]
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
  :handle-ok (lr/ring-response (sc/authorisation-redirect-response sso-config)))
