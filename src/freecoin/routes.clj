;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>
;; Gareth Rogers <grogers@thoughtworks.com>
;; Duncan Mortimer <dmortime@thoughtworks.com>
;; Andrei Biasprozvanny <abiaspro@thoughtworks.com>

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
;;   [liberator.dev]
   [liberator.core :refer [resource defresource]]

   [liberator.representation :refer [as-response ring-response]]
   [compojure.core :refer [defroutes ANY GET POST context]]
   [compojure.route :refer [resources]]

   ;; [environ.core :refer [env]]

   [freecoin.params :as param]

   [freecoin.utils :as util]
   [freecoin.auth :as auth]

   [freecoin.nxt :as nxt]
   [freecoin.wallet :as wallet]
   [freecoin.transactions :as transactions]

   ;; SPIKE: learning about Mozilla persona
   ;; [freecoin.persona-spike :as persona-spike]

   ;; SPIKE: Twitter oauth example
   [freecoin.twitter :as twitter]
   ))



(defn redirect-home [_]
  (liberator.representation/ring-response
   {:body "<html><head><meta http-equiv=\"refresh\" content=\"0; url=/\"></head></html>"
    :headers {"Location" "/"}})
  )

;; debug
(defresource echo [request]
  :allowed-methods [:get]
  :available-media-types ["text/html"]
  ;; :authorized? (:authorised? (auth/check request))
  :authorized? (fn [ctx] (auth/check request))
  :handle-unauthorized (:problem (auth/check request))
;  :as-response (fn [d ctx] (#'cookie-response d ctx))
  :handle-ok (fn [ctx] (util/pretty ctx))

  )

;; routes
(defroutes app

  ;; embedded resources from resources/public
  (compojure.route/resources "/")

  ;; debug
  (ANY "/echo" [request] (echo request))

  (ANY "/version" [] (resource
                      :available-media-types ["text/html" "application/json"]
                      :exists? {::hello  {:Freecoin "D-CENT"
                                          :version param/version
                                          :license "AGPLv3"
                                          :os-name (.. System getProperties (get "os.name"))
                                          :os-version (.. System getProperties (get "os.version"))
                                          :os-arch (.. System getProperties (get "os.arch"))}}

                      :handle-ok #(util/pretty (::hello %))
                      :handle-create #(::hello %)
                      ))

  ;; Wallet operations

  (ANY "/"       [request] (wallet/balance-show request))

  (ANY "/qrcode" [request] (wallet/qrcode request nil))
  (ANY "/qrcode/:name" [name :as request] (wallet/qrcode request name))

  ;;  (ANY "/wallet" [request] (wallet/balance request))
  (GET  "/wallets"                [request] (wallet/get-create request))
  (POST "/wallets"                [request] (wallet/post-create request))
  (GET  "/wallets/:confirmation"  [confirmation :as request]
        (wallet/get-create-confirm request confirmation))
  (POST "/wallets/:confirmation" [confirmation :as request]
        (wallet/post-create-confirm request confirmation))

  ;; Search function
  (GET "/participants" [request] (wallet/participants-form request))
  ;; this will read field and value, url encoded
  (GET "/participants/find" [request] (wallet/participants-find request))
  ;; all is simply find without arguments
  (GET "/participants/all"  [request] (wallet/participants-find request))


  ;; Transactions
  (GET  "/send" [request] (transactions/get-transaction-form request nil))
  (POST "/send" [request] (transactions/post-transaction-form request))

  (GET  "/send/to/:participant" [participant :as request]
        (transactions/get-transaction-form request participant))

  (GET  "/send/confirm/:confirmation" [confirmation :as request]
        (transactions/get-transaction-confirm request confirmation))
  (POST "/send/confirm/:confirmation" [confirmation :as request]
        (transactions/post-transaction-confirm request confirmation))
  (GET  "/transactions/all" [request] (transactions/get-all-transactions request))

  ;; 1 to 1 NXT api mapping for functions taking up to 2 args
  (ANY "/nxt/:command" [command :as request]
       (nxt/api request {"requestType" command}))
  (ANY "/nxt/:cmd1/:key1/:val1" [cmd1 key1 val1 :as request]
       (nxt/api request {"requestType" cmd1 key1 val1}))
  (ANY "/nxt/:cmd1/:key1/:val1/:key2/:val2"
       [cmd1 key1 val1 key2 val2 :as request]
       (nxt/api request {"requestType" cmd1 key1 val1 key2 val2}))

  ;; Persona spike
  ;; persona-spike/routes

  ;; Twitter oauth example
  (context "/twitter" [] twitter/routes)

  ) ; end of routes
