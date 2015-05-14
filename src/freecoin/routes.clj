;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Gareth Rogers <grogers@thoughtworks.com>

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

(ns freecoin.routes
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [liberator.dev]
   [liberator.core :refer [resource defresource]]

   [liberator.representation :refer [as-response ring-response]]
   [compojure.core :refer [defroutes ANY]]

   [environ.core :refer [env]]

   [freecoin.secretshare :as ssss]
   [freecoin.actions :as actions]
   [freecoin.params :as param]
   [freecoin.pages :as pages]
   [freecoin.utils :as util]
   [freecoin.auth :as auth]

   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]
   )
  )

(defn trace []
  (format "<a href=\"%s\">Trace</a>"
          (liberator.dev/current-trace-url))
  )


(defn redirect-home [_]
  (liberator.representation/ring-response
   {:body "<html><head><meta http-equiv=\"refresh\" content=\"0; url=/\"></head></html>"
    :headers {"Location" "/"}})
  )


;; resources
(declare serve)
(declare create)
(declare open)


;; routes
(defroutes app

  (ANY "/"        [request] (serve        request  "Welcome!"))
  (ANY "/version" [request] (serve    request
                                      (str "Freecoin 0.2 running on "
                                           (.. System getProperties (get "os.name"))
                                           " version "
                                           (.. System getProperties (get "os.version"))
                                           " (" (.. System getProperties (get "os.arch")) ")")
                                      )
       )
  (ANY "/create" [request] (create request))
  (ANY "/open/:secret-id" [secret-id :as request] (open request secret-id))

  ) ; end of routes

(defresource create [request]
  :allowed-methods [:get :post]
  :available-media-types ["text/plain" "text/html"]
  :handle-ok (fn [ctx] (str "secrets"))
  :post! (fn [ctx]
           (let [secret (fxc/create-secret param/encryption)
                 cookie-data (str/join "::" [(:cookie secret) (:_id secret)])
                 secret-without-cookie (dissoc secret :cookie)
                 {id :_id} (storage/insert (get-in request [:config :db-connection]) "secrets" secret-without-cookie)]
             {::id id
              ::cookie-data cookie-data}))
  :post-redirect? (fn [ctx] {:location (format "/open/%s" (::id ctx))})
  :handle-see-other (fn [ctx]
                      (ring-response {:headers {"Location" (ctx :location)}
                                      :session {:cookie-data (::cookie-data ctx)}})))

(defresource open [request secret-id]
  :allowed-methods [:get]
  :available-media-types ["text/plain" "text/html"]
  :exists? (fn [ctx]
             (when-let [the-secret (storage/find-by-id
                                    (get-in request [:config :db-connection])
                                    "secrets" secret-id)]
               {::data the-secret}))
  :handle-ok (fn [ctx] (let [cookie (get-in request [:session :cookie-data])
                             slice (first (str/split cookie #"::"))
                             id (second (str/split cookie #"::"))
                             known (::data ctx)
                             quorum (fxc/extract-quorum param/encryption known slice)
                             ah (ssss/shamir-combine (:ah quorum))
                             al (ssss/shamir-combine (:al quorum))
                             nxtpass (fxc/render-slice param/encryption ah al 0)
                             ]
                          (pages/template {:header (trace)
                                           :body (str "DATA: " (::data ctx))
                                           :id (str "NXTPASS: " nxtpass)})
;;                                           :id (str "COOKIE: " (:session request))})
                          )))

(defresource serve [request content]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
;  :as-response (fn [d ctx] (#'cookie-response d ctx))
  :handle-ok (fn [ctx] (let [cookie (get-in request [:session :cookie-data])
                             slice (first (str/split cookie #"::"))
                             id (second (str/split cookie #"::"))]

                         (pages/template {:header (trace)
                                          :body (str content " " id)
                                          :id (str "SLICE: " slice)
                              ;; :id (let [x (auth/parse-secret (get-cookie request))]
                              ;;          (:uid auth/token))
                              }))
                ))
