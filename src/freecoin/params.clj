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

(ns freecoin.params
  (:require [org.httpkit.client :refer [max-body-filter]]
            [environ.core :as env]
            [freecoin.translation :as trans]))

(def version "software release version" "0.2")

;; TODO: perhaps already configured in ring?
(def host
  {:address "fork"
   :port 8000})

(defn- get-mongo-uri []
  (let [mongo-ip (get env/env :mongo-port-27017-tcp-addr "localhost")
        db-name "fxctest1"]
    (format "mongodb://%s:27017/%s" mongo-ip db-name)))

(def webapp
  {:db-config {:url (get-mongo-uri)}

   :host host
   :cookie-config {;; see: https://github.com/ring-clojure/ring/wiki/Cookies
                   :secure false ;restrict the cookie to HTTPS URLs if true
                   :http-only true}})


;; defaults
(def encryption
  {:version 1
   :total 9
   :quorum 5

   :prime 'prime4096

   :description "Freecoin"

   ;; versioning every secret
   :prefix "FXC1"

   ;; random number generator settings
   :length 8
   :entropy 3.1})


(def nxt {:url "https://nxt.dyne.org:7876/nxt"
          :method :post             ; :post :put :head or other
          :user-agent (str "Freecoin" version)
          :headers {"X-header" "value"
                    "X-Api-Version" "2"}
          :keepalive 3000 ; Keep the TCP connection for 3000ms
          :timeout 1000 ; connection and reading timeout 1000ms
          :filter (org.httpkit.client/max-body-filter 1024000)
          ;; reject if body is more than 1MB
          :insecure? true
          ;; Need to contact a server with an untrust

          :max-redirects 10 ; Max redirects to follow
          ;; whether follow 301/302 redirects automatically, default
          ;; to true
          ;; :trace-redirects will contain the chain of the
          ;; redirections followed.
          :follow-redirects true})

(def currency {:code "FXCTA"
               :_id "6010841431981818226"})
