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
   [liberator.dev]
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response]]
   [compojure.core :refer [defroutes ANY]]

   [environ.core :refer [env]]

   [freecoin.secretshare :as ssss]
   [freecoin.actions :as actions]
   [freecoin.pages :as pages]
   [freecoin.auth :as auth]
   [freecoin.db :as db]

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

(defn render-cookie [keys]
  (set-cookie (format "FXC_%s=%s_FXC_%s" (:uuid keys)
                      (first (first  (:shares keys)))
                      (first (second (:shares keys)))
                      ))
  )

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

;; routes
(defroutes app

  (ANY "/"        [request] (serve        request  "Welcome!"))
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

  ) ; end of routes

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

  :handle-ok (let [secret (ssss/create-single ssss/config)]
               (def cookie "")
               ;; debug to console
               ;; (pp/pprint "signup cookies:")
               ;; (pp/pprint (get-cookie request))

               (let [keys (ssss/split ssss/config (:key secret))]
                 (render-cookie keys)
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
               )

  :as-response (fn [d ctx] (#'cookie-response d ctx))
  )


(defresource fake-signup [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]

  :handle-ok (let [secret (ssss/create-single ssss/config)]

               (auth/parse-secret (get-cookie request))

               (let [keys (ssss/split ssss/config (:key secret))]
                 (render-cookie keys)
                 (auth/parse-secret cookie)
                 (liberator.core/log! "Signup" "cookie" cookie)

                 (db/connect)
                 (db/insert {:_id (:uid auth/token)
                             :shares-lo (first  (:shares keys))
                             :shares-hi (second (:shares keys))
                             })
                 (db/disconnect)

                 (pages/signup {:header (trace)
                                :body cookie
                                :id keys
                                })

                 )
               )


  ;; :handle-not-acceptable "Can't hook that handle!"
  ;; :post! (fn [ctx] (#'post-response ctx))
  ;; :post-redirect? true
  ;; :location "http://localhost:8000/login"
  )
