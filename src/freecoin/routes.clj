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
   [liberator.core :refer [resource defresource]]
   [liberator.representation :refer [as-response]]
   [compojure.core :refer [defroutes ANY]]

   [environ.core :refer [env]]

   [freecoin.pages       :as pages]
   )
  )

(def posts (ref []))
(def cookie "TEST=CULOCULOCULO")
(defn set-cookie [d ctx]
  (if-not (empty? cookie)
    (-> (as-response d ctx)
        (assoc-in [:headers "Set-Cookie:"] cookie))
    (-> (as-response d ctx)) ))

;; resources
(declare secret-api)
(declare version)

;; routes
(defroutes app

  (ANY "/version" {cookies :cookies}  (version cookies))
  (ANY "/secret/:ver/:cmd" [ver cmd] (secret-api ver cmd))

  ) ; end of routes






(defresource version [cookies]
  :available-media-types ["text/plain" "text/html"]
  :as-response (fn [d ctx] (#'set-cookie d ctx))

  :handle-ok
  #(let [media-type
         (get-in % [:representation :media-type])]
     (condp = media-type
       "text/plain" (pages/version {:type "txt"
                                    :cookies cookies } )
       "text/html"  (pages/version {:type "html"
                                    :cookies cookies })
       {:message "You requested a media type"
        :media-type media-type}
       )
     )
  :handle-not-acceptable "Can't hook that handle!"
  )


(defresource secret-api [ver cmd]
  :allowed-methods [:post :get]
  :available-media-types ["text/html"]

  :available-media-types ["text/plain"
                          "text/html"
                          "application/json"]
  :handle-ok (fn [ctx]
               (liberator.core/log!
                (format "GET: secret %s %s" ver cmd))
               (format  (str "<html>Post text/plain to this resource.<br>\n"
                                      "There are %d posts at the moment.")
                                 (count @posts))
               )

  :post! (fn [ctx]
           (dosync

            (let [body (slurp (get-in ctx [:request :body]))
                  id   (count (alter posts conj body))]
              {::id id}))

           (liberator.core/log!
            (format "POST: secret %s %s (%d) " ver cmd (count @posts)))
           (format "secret %s %s (%d) " ver cmd (count @posts))

          )

  ;; actually http requires absolute urls for redirect but let's
  ;; keep things simple.
  :post-redirect?
  (fn [ctx] {
             :location (format "http://localhost:3000/version")
             }
    ;; anything executed here invalidates the redirect
    ;; (liberator.core/log!
    ;;  (format "POST: redirect %s" (::request ctx)))
    )

  ) ; end of secret/1 api
