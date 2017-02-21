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
  (:require [taoensso.timbre :as log]
            [bidi.bidi :as bidi]
            [freecoin.config :as config]))

(def routes ["" '(["/" {:get :index}]
                  ["/landing-page" {:get :landing-page}]
                  ["/sign-in-with-sso" {:get :sign-in}]
                  ["/sso-callback" {:get :sso-callback}]
                  ["/sign-out" {:get :sign-out}]
                  ["/forget-secret" {:get :forget-secret}]
                  [["/account/" [#".+\@.+\..+" :email]] {:get :account}]
                  [["/qrcode/" [#".+\@.+\..+" :email]] {:get :qrcode}]
                  [["/transactions/" [#".+\@.+\..+" :email]] {:get :get-user-transactions}]
                  ["/transactions" {:get :get-all-transactions}]
                  ["/tags" {:get :get-all-tags}]
                  ["/participant-query" {:get :get-participant-search-form}]
                  ["/participants" {:get :participants}]
                  ["/send" {:get :get-transaction-form}]
                  ["/send" {:post :post-transaction-form}]
                  [["/send/confirm/" :confirmation-uid] {:get :get-confirm-transaction-form}]
                  [["/send/confirm/" :confirmation-uid] {:post :post-confirm-transaction-form}]
                  ["/activities" {:get :get-activity-streams}]
                  ["/echo" {:get :echo}]
                  ["/version" {:get :version}])])

(defn path [action & params]
  (try
    (apply bidi/path-for routes action params)
    (catch Exception e
      (log/warn (format "Key: '%s' probably does not match a route.\n%s" action e))
      (throw (Exception. (format "Error constructing url for action '%s', with params '%s'" action params))))))

(defn absolute-path [action & params]
  (str (-> config/create-config config/base-url) (apply path action params)))
