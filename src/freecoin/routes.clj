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
  (:use midje.sweet)
  (:require
   [clojure.pprint :as pp]
   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response ring-response]]
   [compojure.core :refer [defroutes ANY]]

   [environ.core :refer [env]]

   [freecoin.secretshare :as ssss]
   [freecoin.actions :as actions]
   [freecoin.pages :as pages]
   [freecoin.utils :as util]
   [freecoin.auth :as auth]
   [freecoin.db :as db]
   [freecoin.storage :as storage]

   [freecoin.fxc :as fxc]
   )
  )

(defn trace []
  (format "<a href=\"%s\">Trace</a>"
          (liberator.dev/current-trace-url))
  )

(def cookie "")

(defn cookie-response [d ctx]
  (if-not (empty? cookie)
    (-> (as-response d ctx)
        (assoc-in [:headers "Set-Cookie:"] cookie))
    (-> (as-response d ctx)) ))

(defn post-response [ctx]
  (dosync
   (let [body (slurp (get-in ctx [:request :body]))]
     (liberator.core/log! "post-response" body)
     )
   true
   )
  )
(defn set-cookie [cooked]
  (def cookie (format "%s; path=/; HttpOnly" cooked))
  ;; Secure flag to be set in production to enforce cookie only over SSL)
  )

(defn get-cookie [req] (get (:headers req) "cookie"))


(defn redirect-home [_]
  (def cookie "")
  (liberator.representation/ring-response
   {:body "<html><head><meta http-equiv=\"refresh\" content=\"0; url=/\"></head></html>"
    :headers {"Location" "/"}})
  )


;; resources
(declare serve)
(declare serve-auth)
(declare signup)
(declare fake-signup)
(declare simple-resource)
(declare tag-session)
(declare secret)
(declare secrets)


;; routes
(defroutes app

  (ANY "/"        [request] (serve        request  "Welcome!"))
  (ANY "/simple-resource" [request] (simple-resource request))
  (ANY "/tag-session" [request] (tag-session request))
  (ANY "/wallet"  [request] (serve-auth   request  "I know you." actions/get-balance))

  (ANY "/signup"  [request] (signup       request))
  (ANY "/fake"    [request] (fake-signup  request))

  (ANY "/version" [request] (serve    request
                                      (str "Freecoin 0.2 running on "
                                           (.. System getProperties (get "os.name"))
                                           " version "
                                           (.. System getProperties (get "os.version"))
                                           " (" (.. System getProperties (get "os.arch")) ")")
                                      )
       )
  (ANY "/secrets" [request] (secrets request))
  (ANY "/secrets/:secret-id" [secret-id :as request] (secret request secret-id))

  ) ; end of routes

(defresource simple-resource [request]
  :allowed-methods [:get]
  :available-media-types ["text/plain"]
  :handle-ok (fn [{:keys [request] :as ctx}]
               (ring-response (as-response (format "The session is: %s" (:session request) )
                                           ctx))))

(defresource tag-session [request]
  :allowed-methods [:get]
  :available-media-types ["text/plain"]
  :exists? (fn [{:keys [request] :as ctx}]
             {::new-dates (cons (java.util.Date.) (get-in request [:session :dates]))})
  :handle-ok (fn [ctx]
               (ring-response (assoc-in (as-response "Session updated." ctx)
                                        [:session :dates] (::new-dates ctx)))))

(defresource secrets [request]
  :allowed-methods [:get :post]
  :available-media-types ["text/plain"]
  :handle-ok (fn [ctx] (str "secrets"))
  :post! (fn [ctx]
           (let [secret (fxc/create-secret ssss/config)
                 {id :_id} (storage/insert (get-in request [:config :db-connection]) "secrets" secret)]
             {::id id}))
  :post-redirect? (fn [ctx] {:location (format "/secrets/%s" (::id ctx))}))

(defresource secret [request secret-id]
  :allowed-methods [:get]
  :available-media-types ["text/plain"]
  :exists? (fn [ctx]
             (when-let [the-secret (storage/find-by-id (get-in request [:config :db-connection]) "secrets" secret-id)]
               {::data the-secret}))
  :handle-ok (fn [ctx] (str (::data ctx))))

(defresource serve [request content]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
;  :as-response (fn [d ctx] (#'cookie-response d ctx))
  :handle-ok (pages/template {:header (trace)
                              :body content
                              :id (let [x (auth/parse-secret (get-cookie request))]
                                       (:uid auth/token))
                              })
  )

(defresource serve-auth [request content action]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
;  :as-response (fn [d ctx] (#'cookie-response d ctx))

  :authorized? (auth/parse-secret (get-cookie request))
  :handle-unauthorized redirect-home

  :handle-ok (let [conn (db/connect)
                   found (db/find-one { :_id (:uid auth/token) } )]

               (db/disconnect)

               (if (nil? found)
                 (pages/template {:header (trace)
                                  :body "I can't remember you, sorry."
                                  })
                 ;; else
                 (let [response (if-not (nil? action) (action found) nil)]
                      (pages/template {:header (trace)
                                       :body response
                                       :id (:uid auth/token)
                                       })
                      )

                 )

               )


  )

(defresource signup [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :authorized? (not (auth/parse-secret (get-cookie request)))
  :handle-unauthorized redirect-home

  :handle-ok (let [secrets (ssss/new-tuple ssss/config)]
               ;; debug to console
               ;; (pp/pprint "signup cookies:")
               ;; (pp/pprint (get-cookie request))
               (auth/render-slice secrets [1])
               (auth/parse-secret cookie)
               (liberator.core/log! "Signup" "cookie" cookie)

                 (db/connect)
                 (db/insert {:_id (:uid auth/token)
                             :shares-lo (first  (:shares keys))
                             :shares-hi (second (:shares keys))
                             })
                 (db/disconnect)

                 (pages/signup {:header (trace)
                                :body "Welcome to Freecoin"
                                :id (:uid auth/token)
                                })
                 )


  :as-response (fn [d ctx] (#'cookie-response d ctx))
  )


(defresource fake-signup [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :handle-ok (let [secret (ssss/new-tuple ssss/config)]

               (auth/parse-secret (get-cookie request))
               (auth/render-slice secret [1])
               (auth/parse-secret cookie)
               (liberator.core/log! "Signup" "cookie" cookie)

               ;; (db/connect)
               ;; (db/insert {:_id (:uid auth/token)
               ;;             :shares-lo (first  (:shares keys))
               ;;             :shares-hi (second (:shares keys))
               ;;             })
               ;; (db/disconnect)

               (pages/signup {:header (trace)
                              :body cookie
                              :id keys
                              })

               )

  ;; :handle-not-acceptable "Can't hook that handle!"
  ;; :post! (fn [ctx] (#'post-response ctx))
  ;; :post-redirect? true
  ;; :location "http://localhost:8000/login"
  )
