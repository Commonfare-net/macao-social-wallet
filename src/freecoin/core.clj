;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>

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

(ns freecoin.core
  (:require
   [org.httpkit.server :as server]
   [liberator.core :refer [resource defresource]]

   ;; comment the following to deactivate debug
   [liberator.dev]

   [ring.middleware.cookies :refer [wrap-cookies]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]

   [cemerick.friend :as friend]

   ;; For Mozilla persona spike
   ;; [persona-kit.middleware :as pm]
   ;; [persona-kit.friend :as pf]
   ;; [persona-kit.uris :as pu]

   [compojure.core :refer [defroutes ANY]]
   [stonecutter-oauth.client :as soc]

   [freecoin.db.mongo :as fm]
   [freecoin.routes :as routes]
   [freecoin.params :as param]
   [freecoin.config :as config]
   [freecoin.storage :as storage]
   [freecoin.secretshare :as ssss]))

(defn wrap-db [handler db-connection]
  (fn [request]
    (handler (assoc-in request [:config :db-connection] db-connection))))

(defn handler [session-configuration db-connection sso-configuration]
  (-> (routes/app db-connection sso-configuration)
      ;; comment the following to deactivate debug
      ;; (liberator.dev/wrap-trace :header :ui)
      (wrap-db db-connection)
      wrap-cookies
      ;;        wrap-anti-forgery
      (wrap-session session-configuration)
      wrap-keyword-params
      wrap-params))

(defonce app-state {})

;; (alter-var-root #'pu/*login-uri* (constantly "/persona/login"))
;; (alter-var-root #'pu/*logout-uri* (constantly "/persona/logout"))

(defn connect-db [app-state]
  (if (:db-connection app-state)
    app-state
    (let [db-config (-> app-state :config-params :db-config)
          db-connection (storage/connect db-config)]
      (assoc app-state :db-connection db-connection))))

(defn disconnect-db [app-state]
  (when (:db-connection app-state)
    (storage/disconnect (:db-connection app-state)))
  (dissoc app-state :db-connection))

(defn start-server [app-state]
  (if (:server app-state)
    app-state
    (let [session-configuration {:cookie-attrs (get-in app-state [:config-params :cookie-config])
                                 :store (cookie-store {:key "sCWg45lZNFNESvPv"})}
          db-connection (:db-connection app-state)
          config-m (config/create-config)
          sso-configuration (soc/configure (config/auth-url config-m)
                                           (config/client-id config-m)
                                           (config/client-secret config-m)
                                           (str (config/base-url config-m) "/sso-callback"))
          server (server/run-server (handler session-configuration db-connection sso-configuration)
                                    {:port 8000})]
      (assoc app-state :server server))))

(defn stop-server [app-state]
  (when-let [server (:server app-state)]
    (server))
  (dissoc app-state :server))

(defn init
  ([app-state] (init app-state param/webapp))

  ([app-state params]
   (assoc app-state :config-params params)))

;; For running from the repl
(defn start []
  (alter-var-root #'app-state
                  #(-> % init connect-db start-server)))

(defn stop []
  (alter-var-root #'app-state
                  #(-> % stop-server disconnect-db)))

;; For running using lein-ring server
(defonce lein-ring-handler nil)

(defn lein-ring-init []
  (alter-var-root #'app-state #(-> % init connect-db))
  (alter-var-root #'lein-ring-handler
                  (fn [_] (let [session-configuration {:cookie-attrs (get-in app-state [:config-params :cookie-config])
                                                       :store (cookie-store {:key "sCWg45lZNFNESvPv"})}
                                db-connection (:db-connection app-state)
                                config-m (config/create-config)
                                sso-configuration (soc/configure (config/auth-url config-m)
                                                                 (config/client-id config-m)
                                                                 (config/client-secret config-m)
                                                                 (str (config/base-url config-m) "/sso-callback"))]
                            (prn "Restarting server....")
                            (handler session-configuration db-connection sso-configuration)))))

(defn lein-ring-stop []
  (alter-var-root #'app-state disconnect-db))

