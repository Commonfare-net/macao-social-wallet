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

(ns freecoin.core
  (:require
   [org.httpkit.server :as server]
   [liberator.core :refer [resource defresource]]

   ;; comment the following to deactivate debug
   [liberator.dev]

   [ring.middleware.cookies        :refer [wrap-cookies]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [compojure.core :refer [defroutes ANY]]
   [freecoin.routes :as routes]
   [freecoin.secretshare :as ssss]
   )
  ;; (:gen-class)
  )

(def handler
  (-> #'routes/app
      ;; comment the following to deactivate debug
      (liberator.dev/wrap-trace :header :ui)
      wrap-cookies
      wrap-keyword-params
      wrap-params)
)

(def the-server (atom nil))

(defn start []
  (reset! the-server (server/run-server handler {:port 8000})))

(defn stop []
  (when @the-server (@the-server)))

