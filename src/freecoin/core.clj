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
  (:require [org.httpkit.server :as server]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [scenic.routes :as scenic]
            [stonecutter-oauth.client :as soc]
            [freecoin.db.mongo :as mongo]
            [freecoin.db.storage :as storage]
            [freecoin.blockchain :as blockchain]
            [freecoin.routes :as routes]
            [freecoin.config :as config]
            [freecoin.handlers.sign-in :as sign-in]
            [freecoin.handlers.account :as account]
            [freecoin.handlers.participant-query :as participant-query]
            [freecoin.handlers.transactions :as transactions]))

(defn not-found [request]
  {:status 404
   :body "Oops! Page not found."})

(defn create-stonecutter-config [config-m]
  (soc/configure (config/auth-url config-m)
                 (config/client-id config-m)
                 (config/client-secret config-m)
                 (routes/absolute-path config-m :sso-callback)))

(defn todo [_]
  {:status 200 :body "Work-in-progress"})

(defn handlers [config-m stores-m blockchain]
  (let [wallet-store (storage/get-wallet-store stores-m)
        confirmation-store (storage/get-confirmation-store stores-m)
        sso-configuration (create-stonecutter-config config-m)]
    (when (= :invalid-configuration sso-configuration)
      (throw (Exception. "Invalid stonecutter configuration. Application launch aborted.")))
    {:index                         (account/balance-page wallet-store blockchain)
     :qrcode                        todo
     :landing-page                  (sign-in/landing-page wallet-store blockchain)
     :sign-in                       (sign-in/sign-in sso-configuration)
     :sso-callback                  (sign-in/sso-callback wallet-store blockchain sso-configuration)
     :get-participant-search-form   (participant-query/query-form wallet-store)
     :participants                  (participant-query/participants wallet-store)
     :get-transaction-form          (transactions/get-transaction-form wallet-store)
     :post-transaction-form         (transactions/post-transaction-form wallet-store confirmation-store)
     :get-confirm-transaction-form  (transactions/get-confirm-transaction-form confirmation-store)
     :transactions                  todo
     :nxt                           todo}))

(defn create-app [config-m stores-m blockchain]
  (-> (scenic/scenic-handler routes/routes (handlers config-m stores-m blockchain) not-found)
      wrap-cookies
      (wrap-session {:cookie-attrs {:secure    false
                                    :http-only true}
                     :store (cookie-store (config/cookie-secret config-m))})
      wrap-keyword-params
      wrap-params))

;; launching and halting the app
(defonce app-state {})

(defn connect-db [app-state]
  (if (:db app-state)
    app-state
    (let [config-m (config/create-config)
          db (-> config-m config/mongo-uri mongo/get-mongo-db)]
      (assoc app-state :db db))))

(defn disconnect-db [app-state]
  (when (:db app-state)
    (prn "WIP: disconnect db"))
  (dissoc app-state :db))

(defn launch [app-state]
  (if (:server app-state)
    app-state
    (if-let [db (:db app-state)]
      (let [config-m (config/create-config)
            stores-m (storage/create-mongo-stores db)
            blockchain (blockchain/new-stub db)
            server (-> (create-app config-m stores-m blockchain)
                       (server/run-server {:port (config/port config-m)
                                           :host (config/host config-m)}))]
        (assoc app-state :server server)))))

(defn halt [app-state]
  (when-let [server (app-state)]
    (server))
  (dissoc app-state :server))

;; For running from the repl
(defn start []
  (alter-var-root #'app-state (comp launch connect-db)))

(defn stop []
  (alter-var-root #'app-state (comp disconnect-db halt)))

;; For running using lein-ring server
(defonce lein-ring-handler nil)

(defn lein-ring-init []
  (prn "lein-ring-init")
  (alter-var-root #'app-state connect-db)
  (alter-var-root #'lein-ring-handler
                  (fn [_] (let [config-m (config/create-config)
                                db (:db app-state)
                                stores-m (storage/create-mongo-stores db)
                                blockchain (blockchain/new-stub db)]
                            (prn "Restarting server....")
                            (create-app config-m stores-m blockchain)))))

(defn lein-ring-stop []
  (alter-var-root #'app-state disconnect-db))

