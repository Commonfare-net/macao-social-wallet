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

(ns freecoin.twitter
  (:require [oauth.client :as oauth]
            [ring.util.response :as response]
            [compojure.core :refer [defroutes GET]]
            [environ.core :refer [env]]
            [freecoin.utils :as utils]
            [freecoin.params :as params]))

(def host-url (str "http://" (get params/host :address) ":" (get params/host :port)))

(def twitter-configuration
  (let [consumer-token (env :twitter-consumer-token)
        secret-token (env :twitter-secret-token)]
    (if (and consumer-token secret-token)
      {:consumer (oauth/make-consumer consumer-token
                                      secret-token
                                      "https://api.twitter.com/oauth/request_token"
                                      "https://api.twitter.com/oauth/access_token"
                                      "https://api.twitter.com/oauth/authenticate"
                                      :hmac-sha1)
       :callback-url (str host-url "/twitter" "/callback")}
      :invalid-configuration)))

(defn invalid-config-handler [request]
  (utils/pretty "Twitter oauth has been incorrectly configured.  Contact your site admin."))

(defn twitter-login-handler [config]
  (if (= config :invalid-configuration)
    invalid-config-handler

    (fn [request]
      (try
        (let [request-token-response (oauth/request-token (:consumer config) (:callback-url config))
              approval-uri (oauth/user-approval-uri (:consumer config) (:oauth_token request-token-response))]
          (response/redirect approval-uri))
        (catch clojure.lang.ExceptionInfo e
          (do (prn (str "Could not get request token from twitter: " e))
              {:status 502}))))))

(defn twitter-callback-handler [config]
  (if (= config :invalid-configuration)
    invalid-config-handler
    (fn [{:keys [params] :as request}]
      (try
        (let [twitter-response (oauth/access-token (:consumer config)
                                                   params
                                                   (:oauth_verifier params))
              twitter-user-id (:user_id twitter-response)
              twitter-screen-name (:screen_name twitter-response)]
          (utils/pretty {:twitter-id twitter-user-id
                         :twitter-screen-name twitter-screen-name}))
        (catch clojure.lang.ExceptionInfo e
          (do (prn (str "Did not get authentication from twitter: " e))
              (response/redirect "/")))))))

(defroutes routes
  (GET "/login" [] (twitter-login-handler twitter-configuration))
  (GET "/callback" [request] (twitter-callback-handler twitter-configuration)))
