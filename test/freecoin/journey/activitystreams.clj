;; Freecoin - digital social currency toolkit

;; part of Decentralized Citizen Engagement Technologies (D-CENT)
;; R&D funded by the European Commission (FP7/CAPS 610349)

;; Copyright (C) 2015 Dyne.org foundation
;; Copyright (C) 2015 Thoughtworks, Inc.

;; Sourcecode designed, written and maintained by
;; Denis Roio <jaromil@dyne.org>

;; With contributions by
;; Arjan Scherpenisse <arjan@scherpenisse.net>

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

(ns freecoin.journey.activitystreams
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [freecoin.blockchain :as blockchain]
            [freecoin.config :as c]
            [freecoin.db.storage :as s]
            [freecoin.journey.kerodon-checkers :as kc]
            [freecoin.journey.kerodon-helpers :as kh]
            [freecoin.journey.kerodon-selectors :as ks]
            [freecoin.routes :as routes]
            [freecoin.test-helpers.integration :as ih]
            [kerodon.core :as k]
            [midje.sweet :refer :all]
            [simple-time.core :as time]
            [stonecutter-oauth.client :as soc]))

(ih/setup-db)

(def stores-m (s/create-mongo-stores (ih/get-test-db)))
(def blockchain (blockchain/new-stub (ih/get-test-db)))

(def test-app (ih/build-app {:stores-m stores-m
                             :blockchain blockchain}))

(background
 (soc/request-access-token! anything "sender") => {:user-info {:sub "sender"
                                                               :email "sender@email.com"}}
 (soc/request-access-token! anything "recipient") => {:user-info {:sub "recipient"
                                                                  :email "recipient@email.com"}})

(defn sign-up [state auth-code]
  (-> state
      (k/visit (str (routes/absolute-path :sso-callback) "?code=" auth-code))
      (kc/check-and-follow-redirect "to account page")))

(def sign-in sign-up)

(defn sign-out [state]
  (k/visit state (routes/absolute-path :sign-out)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn test-activity [from-name amount to-name]
  {"@context"  "https://www.w3.org/ns/activitystreams"
   "@type"     "Transaction"
   "published" (time/format (time/datetime))
   "actor"     {"@type"       "Person"
                "displayName" from-name}
   "object"    {"@type"       "STUB"
                "displayName" (str amount " -> " to-name)}})

(defn no-timestamps [activity] (dissoc activity "published"))

(defn assert-timeless-activitystream [state contents]
  (fact {:midje/name "Checking JSON contents:"}
        (let [output (-> state :response :body cheshire/parse-string vec)]
          (map no-timestamps output) => (map no-timestamps contents)))
  state)


(facts "Activitystreams can be consumed"
       (let [memory (atom {})]
         (-> (k/session test-app)

             (sign-up "recipient")
             (kh/remember memory :recipient-uid kh/state-on-account-page->uid)
             sign-out

             (sign-up "sender")
             (kh/remember memory :sender-uid kh/state-on-account-page->uid)

             ;; do a transaction
             (k/visit (routes/absolute-path :get-transaction-form))
             (kc/check-and-fill-in ks/transaction-form--recipient "recipient")
             (kc/check-and-fill-in ks/transaction-form--amount "10")
             (kc/check-and-press ks/transaction-form--submit)

             (kc/check-and-follow-redirect "to confirm transaction")
             (kc/check-and-press ks/confirm-transaction-form--submit)

             (kc/check-and-follow-redirect "to sender's account page")

             ;; visit the activitystreams page
             (k/visit (routes/absolute-path :get-activity-streams))
             (kc/check-page-is-json :get-activity-streams)

             ;; TODO: fix the activitystreams content type
             ;; #_(kc/content-type-is "application/activity+json;charset=utf-8")

             (assert-timeless-activitystream [(test-activity "sender" 10 "recipient")])

             )))
